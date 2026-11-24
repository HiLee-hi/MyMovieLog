public struct GetRecordsByDateUseCase: Sendable {
    private let repository: any MovieRecordRepository

    public init(repository: any MovieRecordRepository) {
        self.repository = repository
    }

    public func callAsFunction(date: LocalDate) -> AsyncStream<[MovieRecord]> {
        repository.getRecordsByDate(date)
    }
}
