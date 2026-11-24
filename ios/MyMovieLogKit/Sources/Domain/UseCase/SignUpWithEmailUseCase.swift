public struct SignUpWithEmailUseCase: Sendable {
    private let repository: any AuthRepository

    public init(repository: any AuthRepository) {
        self.repository = repository
    }

    public func callAsFunction(email: String, password: String) async throws {
        try await repository.signUpWithEmail(email: email, password: password)
    }
}
