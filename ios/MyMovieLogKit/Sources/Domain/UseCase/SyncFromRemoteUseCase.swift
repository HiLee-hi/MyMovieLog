public struct SyncFromRemoteUseCase: Sendable {
    private let repository: any MovieRecordRepository

    public init(repository: any MovieRecordRepository) {
        self.repository = repository
    }

    public func callAsFunction() async throws {
        try await repository.syncFromRemote()
    }
}
