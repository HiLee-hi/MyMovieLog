public struct GetMovieDetailUseCase: Sendable {
    private let repository: any MovieRepository

    public init(repository: any MovieRepository) {
        self.repository = repository
    }

    public func callAsFunction(movieId: Int) async throws -> Movie {
        try await repository.getMovieDetail(movieId: movieId)
    }
}
