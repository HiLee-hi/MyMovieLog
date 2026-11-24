// public holidays (공공데이터포털 특일 정보), cached per month for offline
public protocol HolidayRepository: Sendable {
    // empty when the key is missing or the request fails
    func holidays(in month: YearMonth) async -> Set<LocalDate>
}
