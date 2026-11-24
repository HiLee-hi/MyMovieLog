public struct GetCurrentUserIdUseCase: Sendable {
    private let repository: any AuthRepository

    public init(repository: any AuthRepository) {
        self.repository = repository
    }

    // throw instead of an empty id, RLS would reject it anyway
    public func callAsFunction() async throws -> String {
        guard let userId = await repository.currentUserId(), !userId.isEmpty else {
            throw DomainError.notSignedIn
        }
        return userId
    }
}
