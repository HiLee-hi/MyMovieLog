import Domain
import Foundation

final class MovieRecordRepositoryImpl: MovieRecordRepository {
    private let remote: any MovieRecordRemoteDataSource
    private let cache: RecordCache

    init(remote: any MovieRecordRemoteDataSource, cache: RecordCache) {
        self.remote = remote
        self.cache = cache
    }

    // Read from local cache (offline support)

    func getRecords(status: WatchStatus?) -> AsyncStream<[MovieRecord]> {
        cache.observe { records in
            guard let status else { return records }
            return records.filter { $0.status == status }
        }
    }

    func getRecordByTmdbId(_ tmdbId: Int) -> AsyncStream<MovieRecord?> {
        cache.observe { records in records.first { $0.tmdbId == tmdbId } }
    }

    func getRecordsByDate(_ date: LocalDate) -> AsyncStream<[MovieRecord]> {
        cache.observe { records in
            records.filter { $0.status == .watched && $0.watchedAt == date }
        }
    }

    func getWatchedDatesByMonth(_ month: YearMonth) -> AsyncStream<Set<LocalDate>> {
        cache.observe { records in
            Set(records.compactMap { record -> LocalDate? in
                guard record.status == .watched, let date = record.watchedAt, month.contains(date)
                else { return nil }
                return date
            })
        }
    }

    // Save to Supabase first, then update local cache

    func upsertRecord(_ record: MovieRecord) async throws {
        Log.record.info("Upsert: tmdbId=\(record.tmdbId), status=\(record.status.rawValue, privacy: .public)")
        do {
            let saved = try await remote.upsert(record.toDto())
            await cache.upsert(saved.toDomain())
            Log.record.info("Upsert success: id=\(saved.id?.prefix(8) ?? "-", privacy: .public)")
        } catch {
            Log.record.error("Upsert failed: tmdbId=\(record.tmdbId), error=\(error.localizedDescription, privacy: .public)")
            throw error
        }
    }

    func deleteRecord(id: String) async throws {
        Log.record.info("Delete: id=\(id.prefix(8), privacy: .public)")
        do {
            try await remote.delete(id: id)
            await cache.delete(id: id)
        } catch {
            Log.record.error("Delete failed: \(error.localizedDescription, privacy: .public)")
            throw error
        }
    }

    // Full sync from Supabase on login or manual refresh. Rows deleted on another device
    // disappear here too because the cache is replaced, not merged.
    func syncFromRemote() async throws {
        Log.record.info("Sync from remote started")
        do {
            let records = try await remote.fetchAll()
            await cache.replaceAll(records.map { $0.toDomain() })
            Log.record.info("Sync from remote success: count=\(records.count)")
        } catch {
            Log.record.error("Sync from remote failed: \(error.localizedDescription, privacy: .public)")
            throw error
        }
    }

    func clearLocalCache() async throws {
        Log.record.info("Clearing local cache")
        await cache.clear()
    }
}
