/// Korean public holidays (공공데이터포털 특일 정보), cached per month so the calendar works offline.
public protocol HolidayRepository: Sendable {
    /// Holidays of `month`; empty when the API key is missing or the request fails
    func holidays(in month: YearMonth) async -> Set<LocalDate>
}
