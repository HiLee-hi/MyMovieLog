public struct DeleteRecordUseCase: Sendable {
    private let repository: any MovieRecordRepository

    public init(repository: any MovieRecordRepository) {
        self.repository = repository
    }

    public func callAsFunction(recordId: String) async throws {
        try await repository.deleteRecord(id: recordId)
    }
}
