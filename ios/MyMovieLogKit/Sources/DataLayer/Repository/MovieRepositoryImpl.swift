import Domain

final class MovieRepositoryImpl: MovieRepository {
    private let api: TmdbApi

    init(api: TmdbApi) {
        self.api = api
    }

    func searchMovies(query: String, page: Int) async throws -> MoviePage {
        Log.movie.debug("Search: page=\(page)")
        let response = try await api.searchMovies(query: query, page: page)
        return MoviePage(
            movies: response.results.map { $0.toDomain() },
            page: response.page,
            totalPages: response.totalPages
        )
    }

    func getMovieDetail(movieId: Int) async throws -> Movie {
        try await api.getMovieDetail(movieId: movieId).toDomain()
    }
}
