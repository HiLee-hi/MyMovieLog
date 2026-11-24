public struct ClearLocalCacheUseCase: Sendable {
    private let repository: any MovieRecordRepository

    public init(repository: any MovieRecordRepository) {
        self.repository = repository
    }

    public func callAsFunction() async throws {
        try await repository.clearLocalCache()
    }
}
