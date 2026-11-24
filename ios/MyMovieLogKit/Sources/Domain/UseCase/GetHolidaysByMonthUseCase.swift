public struct GetHolidaysByMonthUseCase: Sendable {
    private let repository: any HolidayRepository

    public init(repository: any HolidayRepository) {
        self.repository = repository
    }

    public func callAsFunction(month: YearMonth) async -> Set<LocalDate> {
        await repository.holidays(in: month)
    }
}
