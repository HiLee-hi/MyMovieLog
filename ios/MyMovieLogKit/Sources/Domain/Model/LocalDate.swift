import Foundation

// date without time / zone, like java.time.LocalDate
// `watched_at` is a date column ("yyyy-MM-dd"), so time zone changes don't move records
public struct LocalDate: Hashable, Comparable, Sendable {
    public let year: Int
    public let month: Int
    public let day: Int

    public init(year: Int, month: Int, day: Int) {
        self.year = year
        self.month = month
        self.day = day
    }

    // "yyyy-MM-dd", longer timestamps use the date part
    public init?(isoString: String) {
        let parts = isoString.prefix(10).split(separator: "-")
        guard parts.count == 3,
              let year = Int(parts[0]), let month = Int(parts[1]), let day = Int(parts[2]),
              (1...12).contains(month),
              (1...YearMonth(year: year, month: month).numberOfDays).contains(day)
        else { return nil }
        self.init(year: year, month: month, day: day)
    }

    public init(_ date: Date, calendar: Calendar = .current) {
        let components = calendar.dateComponents([.year, .month, .day], from: date)
        self.init(year: components.year ?? 1970, month: components.month ?? 1, day: components.day ?? 1)
    }

    public static func today(calendar: Calendar = .current) -> LocalDate {
        LocalDate(Date(), calendar: calendar)
    }

    public var isoString: String {
        String(format: "%04d-%02d-%02d", year, month, day)
    }

    public var yearMonth: YearMonth {
        YearMonth(year: year, month: month)
    }

    // start of day in calendar, for pickers / formatters
    public func date(calendar: Calendar = .current) -> Date {
        calendar.date(from: DateComponents(year: year, month: month, day: day)) ?? Date()
    }

    public static func < (lhs: LocalDate, rhs: LocalDate) -> Bool {
        (lhs.year, lhs.month, lhs.day) < (rhs.year, rhs.month, rhs.day)
    }
}

// like java.time.YearMonth
public struct YearMonth: Hashable, Comparable, Sendable {
    public let year: Int
    public let month: Int

    public init(year: Int, month: Int) {
        self.year = year
        self.month = month
    }

    public static func current(calendar: Calendar = .current) -> YearMonth {
        LocalDate.today(calendar: calendar).yearMonth
    }

    public func adding(months: Int) -> YearMonth {
        let index = year * 12 + (month - 1) + months
        return YearMonth(year: index / 12, month: index % 12 + 1)
    }

    public var numberOfDays: Int {
        switch month {
        case 2: return isLeapYear ? 29 : 28
        case 4, 6, 9, 11: return 30
        default: return 31
        }
    }

    // weekday of the 1st, 1 = Sunday ... 7 = Saturday
    public var firstWeekday: Int {
        // Zeller-style, independent of the device calendar
        let shifted = month < 3 ? (year - 1, month + 12) : (year, month)
        let k = shifted.0 % 100
        let j = shifted.0 / 100
        let h = (1 + (13 * (shifted.1 + 1)) / 5 + k + k / 4 + j / 4 + 5 * j) % 7
        // h: 0 = Saturday, 1 = Sunday, ...
        return (h + 6) % 7 + 1
    }

    public func day(_ day: Int) -> LocalDate {
        LocalDate(year: year, month: month, day: day)
    }

    public func contains(_ date: LocalDate) -> Bool {
        date.year == year && date.month == month
    }

    private var isLeapYear: Bool {
        (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    }

    public static func < (lhs: YearMonth, rhs: YearMonth) -> Bool {
        (lhs.year, lhs.month) < (rhs.year, rhs.month)
    }
}
