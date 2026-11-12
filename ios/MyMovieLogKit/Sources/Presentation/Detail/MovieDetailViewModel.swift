import Domain
import Observation

public enum DetailPhase: Equatable, Sendable {
    case loading
    case loaded(Movie)
    case error(String)
}

@MainActor
@Observable
public final class MovieDetailViewModel {
    public let movieId: Int
    public private(set) var phase: DetailPhase = .loading
    public private(set) var existingRecord: MovieRecord?

    public var isEditorPresented = false
    public var draft = RecordDraft()
    public private(set) var saveState: SaveState = .idle

    private let getMovieDetail: GetMovieDetailUseCase
    private let getRecordByTmdbId: GetRecordByTmdbIdUseCase
    private let upsertRecord: UpsertRecordUseCase
    private let getCurrentUserId: GetCurrentUserIdUseCase

    public init(
        movieId: Int,
        getMovieDetail: GetMovieDetailUseCase,
        getRecordByTmdbId: GetRecordByTmdbIdUseCase,
        upsertRecord: UpsertRecordUseCase,
        getCurrentUserId: GetCurrentUserIdUseCase
    ) {
        self.movieId = movieId
        self.getMovieDetail = getMovieDetail
        self.getRecordByTmdbId = getRecordByTmdbId
        self.upsertRecord = upsertRecord
        self.getCurrentUserId = getCurrentUserId
    }

    public var movie: Movie? {
        if case .loaded(let movie) = phase { return movie }
        return nil
    }

    /// Runs again each time the screen appears; only fetches until it has succeeded once
    public func loadDetail() async {
        guard movie == nil else { return }
        phase = .loading
        do {
            phase = .loaded(try await getMovieDetail(movieId: movieId))
        } catch {
            guard !Task.isCancelled else { return }
            phase = .error(error.localizedDescription)
        }
    }

    public func observeRecord() async {
        for await record in getRecordByTmdbId(tmdbId: movieId) {
            existingRecord = record
        }
    }

    public func openEditor() {
        draft = RecordDraft(record: existingRecord)
        saveState = .idle
        isEditorPresented = true
    }

    public func dismissEditor() {
        isEditorPresented = false
        saveState = .idle
    }

    public func saveRecord() async {
        guard let movie, !saveState.isSaving else { return }
        saveState = .saving
        do {
            let userId = try await getCurrentUserId()
            let existing = existingRecord
            // Movie metadata is refreshed from TMDB; photos attached on Android are kept
            let base = MovieRecord(
                id: existing?.id ?? "",
                userId: userId,
                tmdbId: movie.id,
                title: movie.title,
                originalTitle: movie.originalTitle,
                posterPath: movie.posterPath,
                genreIds: movie.genreIds,
                status: draft.status,
                photoUrls: existing?.photoUrls ?? [],
                createdAt: existing?.createdAt ?? ""
            )
            try await upsertRecord(draft.applied(to: base))
            saveState = .success
            isEditorPresented = false
        } catch {
            saveState = .error(error.localizedDescription)
        }
    }
}
