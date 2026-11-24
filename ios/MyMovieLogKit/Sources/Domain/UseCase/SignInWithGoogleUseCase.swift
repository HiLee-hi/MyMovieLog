public struct SignInWithGoogleUseCase: Sendable {
    private let repository: any AuthRepository

    public init(repository: any AuthRepository) {
        self.repository = repository
    }

    public func callAsFunction() async throws {
        try await repository.signInWithGoogle()
    }
}
