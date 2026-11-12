import Domain
import Observation

public enum SearchPhase: Equatable, Sendable {
    case idle
    case loading
    case loaded
    case empty
    case error(String)
}

@MainActor
@Observable
public final class SearchViewModel {
    /// Bound to the search field; the view debounces changes and then calls `search()`
    public var query = ""
    public private(set) var movies: [Movie] = []
    public private(set) var phase: SearchPhase = .idle
    public private(set) var isLoadingMore = false

    private var searchedQuery = ""
    private var lastPage = 0
    private var totalPages = 0

    private let searchMovies: SearchMoviesUseCase

    public init(searchMovies: SearchMoviesUseCase) {
        self.searchMovies = searchMovies
    }

    public func search() async {
        let trimmed = query.trimmingCharacters(in: .whitespacesAndNewlines)
        guard trimmed != searchedQuery || phase.isRetryable else { return }
        searchedQuery = trimmed
        movies = []
        lastPage = 0
        totalPages = 0
        guard !trimmed.isEmpty else {
            phase = .idle
            return
        }
        phase = .loading
        do {
            let page = try await searchMovies(query: trimmed, page: 1)
            guard !Task.isCancelled, trimmed == searchedQuery else { return }
            movies = page.movies
            lastPage = page.page
            totalPages = page.totalPages
            phase = movies.isEmpty ? .empty : .loaded
        } catch {
            guard !Task.isCancelled, trimmed == searchedQuery else { return }
            phase = .error(error.localizedDescription)
        }
    }

    /// Loads the next page when the last row appears, like Paging 3 on Android
    public func loadMoreIfNeeded(after movie: Movie) async {
        guard movie.id == movies.last?.id, lastPage < totalPages, !isLoadingMore else { return }
        let query = searchedQuery
        isLoadingMore = true
        defer { isLoadingMore = false }
        do {
            let page = try await searchMovies(query: query, page: lastPage + 1)
            guard query == searchedQuery else { return }
            // TMDB pages can overlap when results shift between requests
            let known = Set(movies.map(\.id))
            movies += page.movies.filter { !known.contains($0.id) }
            lastPage = page.page
            totalPages = page.totalPages
        } catch {
            // Scrolling to the end again retries
        }
    }

    public func clearSearch() {
        query = ""
        searchedQuery = ""
        movies = []
        phase = .idle
    }
}

private extension SearchPhase {
    var isRetryable: Bool {
        if case .error = self { return true }
        return false
    }
}
