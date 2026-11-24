public struct ObserveCurrentUserUseCase: Sendable {
    private let repository: any AuthRepository

    public init(repository: any AuthRepository) {
        self.repository = repository
    }

    public func callAsFunction() -> AsyncStream<UserProfile?> {
        repository.observeCurrentUser()
    }
}
