public struct GetRecordsUseCase: Sendable {
    private let repository: any MovieRecordRepository

    public init(repository: any MovieRecordRepository) {
        self.repository = repository
    }

    public func callAsFunction(status: WatchStatus? = nil) -> AsyncStream<[MovieRecord]> {
        repository.getRecords(status: status)
    }
}
