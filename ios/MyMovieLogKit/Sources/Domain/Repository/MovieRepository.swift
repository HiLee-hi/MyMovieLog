public protocol MovieRepository: Sendable {
    func searchMovies(query: String, page: Int) async throws -> MoviePage
    func getMovieDetail(movieId: Int) async throws -> Movie
}
