@testable import DataLayer
import Domain
import Foundation
import Testing

private actor FakeRemote: MovieRecordRemoteDataSource {
    var rows: [MovieRecordDto] = []
    var nextId = 1
    var failNext = false

    func setRows(_ rows: [MovieRecordDto]) { self.rows = rows }
    func failNextCall() { failNext = true }

    func upsert(_ record: MovieRecordDto) async throws -> MovieRecordDto {
        if failNext { failNext = false; throw URLError(.notConnectedToInternet) }
        var saved = record
        if saved.id == nil {
            saved.id = "id-\(nextId)"
            saved.createdAt = String(format: "2026-10-%02dT00:00:00Z", nextId)
            nextId += 1
        }
        rows.removeAll { $0.id == saved.id || ($0.userId == saved.userId && $0.tmdbId == saved.tmdbId) }
        rows.append(saved)
        return saved
    }

    func delete(id: String) async throws {
        rows.removeAll { $0.id == id }
    }

    func fetchAll() async throws -> [MovieRecordDto] { rows }
}

/// Waits for the first value that satisfies `predicate`
private func firstValue<T: Sendable>(
    of stream: AsyncStream<T>,
    where predicate: @Sendable (T) -> Bool = { _ in true }
) async -> T? {
    for await value in stream where predicate(value) {
        return value
    }
    return nil
}

struct MovieRecordRepositoryTests {
    private let remote = FakeRemote()

    private func makeRepository(fileURL: URL? = nil) -> MovieRecordRepositoryImpl {
        MovieRecordRepositoryImpl(remote: remote, cache: RecordCache(fileURL: fileURL))
    }

    private func record(_ tmdbId: Int, _ status: WatchStatus, watchedAt: LocalDate? = nil) -> MovieRecord {
        MovieRecord(userId: "u", tmdbId: tmdbId, title: "movie \(tmdbId)", status: status, watchedAt: watchedAt)
    }

    @Test func upsertStoresServerRowInCache() async throws {
        let repository = makeRepository()
        try await repository.upsertRecord(record(1, .watched))

        let cached = await firstValue(of: repository.getRecordByTmdbId(1)) { $0 != nil }
        #expect(cached??.id == "id-1")
        #expect(cached??.createdAt.isEmpty == false)
    }

    @Test func failedUpsertLeavesCacheUntouched() async throws {
        let repository = makeRepository()
        await remote.failNextCall()
        await #expect(throws: URLError.self) {
            try await repository.upsertRecord(record(1, .watched))
        }
        let records = await firstValue(of: repository.getRecords(status: nil))
        #expect(records?.isEmpty == true)
    }

    @Test func updatingRecordReplacesInsteadOfDuplicating() async throws {
        let repository = makeRepository()
        try await repository.upsertRecord(record(1, .wishlist))
        var saved = try #require(await firstValue(of: repository.getRecordByTmdbId(1)) { $0 != nil } ?? nil)
        saved.status = .watched
        try await repository.upsertRecord(saved)

        let all = await firstValue(of: repository.getRecords(status: nil)) { $0.first?.status == .watched }
        #expect(all?.count == 1)
    }

    @Test func filtersByStatusDateAndMonth() async throws {
        let repository = makeRepository()
        let oct3 = LocalDate(year: 2026, month: 10, day: 3)
        let nov1 = LocalDate(year: 2026, month: 11, day: 1)
        try await repository.upsertRecord(record(1, .watched, watchedAt: oct3))
        try await repository.upsertRecord(record(2, .watched, watchedAt: nov1))
        try await repository.upsertRecord(record(3, .wishlist))

        let watched = await firstValue(of: repository.getRecords(status: .watched))
        #expect(watched?.map(\.tmdbId).sorted() == [1, 2])

        let onOct3 = await firstValue(of: repository.getRecordsByDate(oct3))
        #expect(onOct3?.map(\.tmdbId) == [1])

        let octoberDates = await firstValue(of: repository.getWatchedDatesByMonth(YearMonth(year: 2026, month: 10)))
        #expect(octoberDates == [oct3])
    }

    @Test func recordsAreNewestFirst() async throws {
        let repository = makeRepository()
        try await repository.upsertRecord(record(1, .watched))
        try await repository.upsertRecord(record(2, .watched))
        let records = await firstValue(of: repository.getRecords(status: nil)) { $0.count == 2 }
        #expect(records?.map(\.tmdbId) == [2, 1])
    }

    @Test func syncReplacesCacheWithRemoteRows() async throws {
        let repository = makeRepository()
        try await repository.upsertRecord(record(1, .watched))
        await remote.setRows([record(9, .wishlist).toDto()])

        try await repository.syncFromRemote()
        let records = await firstValue(of: repository.getRecords(status: nil)) { $0.first?.tmdbId == 9 }
        #expect(records?.map(\.tmdbId) == [9])
    }

    @Test func observersSeeDeletesAndClears() async throws {
        let repository = makeRepository()
        try await repository.upsertRecord(record(1, .watched))
        try await repository.upsertRecord(record(2, .watched))
        let first = try #require(await firstValue(of: repository.getRecordByTmdbId(1)) { $0 != nil } ?? nil)

        try await repository.deleteRecord(id: first.id)
        let afterDelete = await firstValue(of: repository.getRecords(status: nil)) { $0.count == 1 }
        #expect(afterDelete?.map(\.tmdbId) == [2])

        try await repository.clearLocalCache()
        let afterClear = await firstValue(of: repository.getRecords(status: nil)) { $0.isEmpty }
        #expect(afterClear?.isEmpty == true)
    }

    @Test func cachePersistsAcrossLaunches() async throws {
        let fileURL = FileManager.default.temporaryDirectory
            .appending(path: "record-cache-\(UUID().uuidString).json")
        defer { try? FileManager.default.removeItem(at: fileURL) }

        try await makeRepository(fileURL: fileURL).upsertRecord(
            record(1, .watched, watchedAt: LocalDate(year: 2026, month: 10, day: 3))
        )

        let relaunched = makeRepository(fileURL: fileURL)
        let records = await firstValue(of: relaunched.getRecords(status: nil))
        #expect(records?.first?.tmdbId == 1)
        #expect(records?.first?.watchedAt == LocalDate(year: 2026, month: 10, day: 3))
    }
}

struct AppConfigTests {
    @Test func readsInfoDictionary() {
        let config = AppConfig.fromInfoDictionary([
            "TMDB_API_KEY": "token",
            "SUPABASE_URL": "https://abc.supabase.co",
            "SUPABASE_ANON_KEY": "anon"
        ])
        #expect(config?.supabaseURL.host() == "abc.supabase.co")
    }

    @Test func rejectsMissingOrUnexpandedValues() {
        #expect(AppConfig.fromInfoDictionary([:]) == nil)
        #expect(AppConfig.fromInfoDictionary([
            "TMDB_API_KEY": "$(TMDB_API_KEY)",
            "SUPABASE_URL": "https://abc.supabase.co",
            "SUPABASE_ANON_KEY": "anon"
        ]) == nil)
    }
}
