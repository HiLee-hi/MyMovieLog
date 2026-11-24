public struct SignInWithEmailUseCase: Sendable {
    private let repository: any AuthRepository

    public init(repository: any AuthRepository) {
        self.repository = repository
    }

    public func callAsFunction(email: String, password: String) async throws {
        try await repository.signInWithEmail(email: email, password: password)
    }
}
