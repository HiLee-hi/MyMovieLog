#if DEBUG
import Domain
import Foundation

// In-memory repositories for SwiftUI previews and ViewModel tests

/// Broadcasts a value to any number of AsyncStream observers
private final class Broadcaster<Value: Sendable>: @unchecked Sendable {
    private let lock = NSLock()
    private var value: Value
    private var continuations: [UUID: AsyncStream<Value>.Continuation] = [:]

    init(_ value: Value) {
        self.value = value
    }

    var current: Value {
        lock.withLock { value }
    }

    func update(_ transform: (inout Value) -> Void) {
        let (newValue, targets) = lock.withLock {
            transform(&value)
            return (value, Array(continuations.values))
        }
        targets.forEach { $0.yield(newValue) }
    }

    func stream<T: Sendable>(_ transform: @escaping @Sendable (Value) -> T) -> AsyncStream<T> {
        let (stream, continuation) = AsyncStream<T>.makeStream()
        let id = UUID()
        let relay = AsyncStream<Value>.makeStream()
        let task = Task {
            for await value in relay.stream { continuation.yield(transform(value)) }
        }
        continuation.onTermination = { [weak self] _ in
            task.cancel()
            self?.lock.withLock { _ = self?.continuations.removeValue(forKey: id) }
        }
        let initial = lock.withLock {
            continuations[id] = relay.continuation
            return value
        }
        relay.continuation.yield(initial)
        return stream
    }
}

final class InMemoryAuthRepository: AuthRepository, @unchecked Sendable {
    private let user: Broadcaster<UserProfile?>
    var signInError: Error?

    init(user: UserProfile? = .preview) {
        self.user = Broadcaster(user)
    }

    func observeCurrentUser() -> AsyncStream<UserProfile?> { user.stream { $0 } }

    func currentUserId() async -> String? { user.current?.id }

    func signInWithEmail(email: String, password: String) async throws {
        if let signInError { throw signInError }
        user.update { $0 = UserProfile(id: "preview-user", email: email, displayName: nil, avatarUrl: nil) }
    }

    func signUpWithEmail(email: String, password: String) async throws {
        if let signInError { throw signInError }
    }

    func signInWithGoogle() async throws {
        user.update { $0 = .preview }
    }

    func signOut() async throws {
        user.update { $0 = nil }
    }
}

final class InMemoryMovieRepository: MovieRepository, @unchecked Sendable {
    let pageSize: Int
    let movies: [Movie]
    private let lock = NSLock()
    private var _requestedPages: [Int] = []

    init(movies: [Movie] = Movie.previewList, pageSize: Int = 20) {
        self.movies = movies
        self.pageSize = pageSize
    }

    var requestedPages: [Int] { lock.withLock { _requestedPages } }

    func searchMovies(query: String, page: Int) async throws -> MoviePage {
        lock.withLock { _requestedPages.append(page) }
        let matches = movies.filter { $0.title.localizedCaseInsensitiveContains(query) || query == "*" }
        let totalPages = max(1, Int((Double(matches.count) / Double(pageSize)).rounded(.up)))
        let pageMovies = Array(matches.dropFirst((page - 1) * pageSize).prefix(pageSize))
        return MoviePage(movies: pageMovies, page: page, totalPages: matches.isEmpty ? 0 : totalPages)
    }

    func getMovieDetail(movieId: Int) async throws -> Movie {
        guard let movie = movies.first(where: { $0.id == movieId }) else { throw URLError(.badServerResponse) }
        return movie
    }
}

final class InMemoryRecordRepository: MovieRecordRepository, @unchecked Sendable {
    private let records: Broadcaster<[MovieRecord]>
    private let lock = NSLock()
    private var _syncCount = 0
    private var _clearCount = 0
    var upsertError: Error?

    init(records: [MovieRecord] = MovieRecord.previewList) {
        self.records = Broadcaster(records)
    }

    var allRecords: [MovieRecord] { records.current }
    var syncCount: Int { lock.withLock { _syncCount } }
    var clearCount: Int { lock.withLock { _clearCount } }

    func getRecords(status: WatchStatus?) -> AsyncStream<[MovieRecord]> {
        records.stream { records in records.filter { status == nil || $0.status == status } }
    }

    func getRecordByTmdbId(_ tmdbId: Int) -> AsyncStream<MovieRecord?> {
        records.stream { records in records.first { $0.tmdbId == tmdbId } }
    }

    func getRecordsByDate(_ date: LocalDate) -> AsyncStream<[MovieRecord]> {
        records.stream { records in records.filter { $0.status == .watched && $0.watchedAt == date } }
    }

    func getWatchedDatesByMonth(_ month: YearMonth) -> AsyncStream<Set<LocalDate>> {
        records.stream { records in
            Set(records.filter { $0.status == .watched }.compactMap(\.watchedAt).filter(month.contains))
        }
    }

    func upsertRecord(_ record: MovieRecord) async throws {
        if let upsertError { throw upsertError }
        records.update { records in
            var saved = record
            if saved.id.isEmpty { saved.id = UUID().uuidString.lowercased() }
            if saved.createdAt.isEmpty { saved.createdAt = ISO8601DateFormatter().string(from: Date()) }
            records.removeAll { $0.id == saved.id || $0.tmdbId == saved.tmdbId }
            records.insert(saved, at: 0)
        }
    }

    func deleteRecord(id: String) async throws {
        records.update { $0.removeAll { $0.id == id } }
    }

    func syncFromRemote() async throws {
        lock.withLock { _syncCount += 1 }
    }

    func clearLocalCache() async throws {
        lock.withLock { _clearCount += 1 }
        records.update { $0 = [] }
    }
}

extension UseCaseContainer {
    static func preview(loggedIn: Bool = true) -> UseCaseContainer {
        UseCaseContainer(
            authRepository: InMemoryAuthRepository(user: loggedIn ? .preview : nil),
            movieRepository: InMemoryMovieRepository(),
            recordRepository: InMemoryRecordRepository()
        )
    }
}

extension UserProfile {
    static let preview = UserProfile(id: "preview-user", email: "movie@example.com", displayName: "영화 애호가", avatarUrl: nil)
}

extension Movie {
    static let previewList: [Movie] = [
        Movie(id: 496243, title: "기생충", originalTitle: "기생충", overview: "전원 백수로 살 길 막막하지만 사이는 좋은 기택 가족.", posterPath: "/mSi0gskYpmf1FbXngM37s2HppXh.jpg", backdropPath: "/hiKmpZMGZsrkA3cdce8a7Dpos1j.jpg", releaseDate: "2019-05-30", voteAverage: 8.5, genreIds: [35, 53, 18], genres: ["코미디", "스릴러", "드라마"]),
        Movie(id: 157336, title: "인터스텔라", originalTitle: "Interstellar", overview: "세계 각국의 정부와 경제가 완전히 붕괴된 미래가 다가온다.", posterPath: "/nCbkOyOMTEwlEV0LtCOvCnwEONA.jpg", backdropPath: "/pbrkL804c8yAv3zBZR4QPEafpAR.jpg", releaseDate: "2014-11-05", voteAverage: 8.4, genreIds: [12, 18, 878], genres: ["모험", "드라마", "SF"]),
        Movie(id: 372058, title: "너의 이름은.", originalTitle: "君の名は。", overview: "아직 만난 적 없는 너를, 찾고 있어.", posterPath: "/tG5ggJVSyXmBCxbNx2jd3AtzM7s.jpg", backdropPath: nil, releaseDate: "2016-08-26", voteAverage: 8.5, genreIds: [16, 10749, 18], genres: ["애니메이션", "로맨스", "드라마"])
    ]
}

extension MovieRecord {
    static let previewList: [MovieRecord] = {
        let today = LocalDate.today()
        return [
            MovieRecord(id: "r1", userId: "preview-user", tmdbId: 496243, title: "기생충", posterPath: "/mSi0gskYpmf1FbXngM37s2HppXh.jpg", status: .watched, rating: 4.5, review: "계단 장면이 오래 남는다.", watchedAt: today, createdAt: "2026-10-03T10:00:00Z"),
            MovieRecord(id: "r2", userId: "preview-user", tmdbId: 157336, title: "인터스텔라", posterPath: "/nCbkOyOMTEwlEV0LtCOvCnwEONA.jpg", status: .watched, rating: 5, watchedAt: today.yearMonth.day(1), createdAt: "2026-10-01T10:00:00Z"),
            MovieRecord(id: "r3", userId: "preview-user", tmdbId: 372058, title: "너의 이름은.", posterPath: "/tG5ggJVSyXmBCxbNx2jd3AtzM7s.jpg", status: .wishlist, memo: "극장 재개봉하면 보기", createdAt: "2026-09-20T10:00:00Z")
        ]
    }()
}
#endif
