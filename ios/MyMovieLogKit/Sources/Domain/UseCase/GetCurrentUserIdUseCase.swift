public struct GetCurrentUserIdUseCase: Sendable {
    private let repository: any AuthRepository

    public init(repository: any AuthRepository) {
        self.repository = repository
    }

    /// Throws instead of returning an empty id, which Supabase RLS would reject anyway
    public func callAsFunction() async throws -> String {
        guard let userId = await repository.currentUserId(), !userId.isEmpty else {
            throw DomainError.notSignedIn
        }
        return userId
    }
}
