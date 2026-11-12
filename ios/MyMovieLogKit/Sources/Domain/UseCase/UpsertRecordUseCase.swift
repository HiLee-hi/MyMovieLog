public struct UpsertRecordUseCase: Sendable {
    private let repository: any MovieRecordRepository

    public init(repository: any MovieRecordRepository) {
        self.repository = repository
    }

    public func callAsFunction(_ record: MovieRecord) async throws {
        try await repository.upsertRecord(record)
    }
}
