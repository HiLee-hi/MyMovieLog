public struct GetWatchedDatesByMonthUseCase: Sendable {
    private let repository: any MovieRecordRepository

    public init(repository: any MovieRecordRepository) {
        self.repository = repository
    }

    public func callAsFunction(month: YearMonth) -> AsyncStream<Set<LocalDate>> {
        repository.getWatchedDatesByMonth(month)
    }
}
