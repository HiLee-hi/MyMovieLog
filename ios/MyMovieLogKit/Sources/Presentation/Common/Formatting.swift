import Domain
import Foundation

extension LocalDate {
    /// "2026년 10월 28일", "Oct 28, 2026"
    var displayText: String {
        formattedDate(year: year, month: month, day: day, pattern: L10n.datePattern)
    }
}

extension YearMonth {
    /// "2026년 10월", "October 2026"
    var displayText: String {
        formattedDate(year: year, month: month, day: 1, pattern: L10n.yearMonthPattern)
    }

    /// "10월", "Oct": a chart axis label
    var monthText: String {
        formattedDate(year: year, month: month, day: 1, pattern: L10n.monthPattern)
    }
}

private func formattedDate(year: Int, month: Int, day: Int, pattern: String) -> String {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = .gmt
    guard let date = calendar.date(from: DateComponents(year: year, month: month, day: day)) else { return "" }

    let formatter = DateFormatter()
    formatter.calendar = calendar
    formatter.timeZone = calendar.timeZone
    formatter.locale = L10n.locale
    formatter.dateFormat = pattern
    return formatter.string(from: date)
}
