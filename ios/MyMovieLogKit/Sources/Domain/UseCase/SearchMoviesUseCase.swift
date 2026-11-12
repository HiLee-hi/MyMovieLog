public struct SearchMoviesUseCase: Sendable {
    private let repository: any MovieRepository

    public init(repository: any MovieRepository) {
        self.repository = repository
    }

    public func callAsFunction(query: String, page: Int = 1) async throws -> MoviePage {
        try await repository.searchMovies(query: query, page: page)
    }
}
