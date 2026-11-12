import Domain
import Testing

struct LocalDateTests {
    @Test func parsesIsoDateAndTimestamp() {
        #expect(LocalDate(isoString: "2026-10-28") == LocalDate(year: 2026, month: 10, day: 28))
        #expect(LocalDate(isoString: "2026-10-28T09:30:00+00:00") == LocalDate(year: 2026, month: 10, day: 28))
    }

    @Test(arguments: ["", "2026-13-01", "2026-02-30", "2025-02-29", "abc"])
    func rejectsInvalidDates(_ text: String) {
        #expect(LocalDate(isoString: text) == nil)
    }

    @Test func formatsWithZeroPadding() {
        #expect(LocalDate(year: 2026, month: 3, day: 7).isoString == "2026-03-07")
    }

    @Test func ordersChronologically() {
        #expect(LocalDate(year: 2025, month: 12, day: 31) < LocalDate(year: 2026, month: 1, day: 1))
    }
}

struct YearMonthTests {
    @Test func addsMonthsAcrossYears() {
        #expect(YearMonth(year: 2026, month: 12).adding(months: 1) == YearMonth(year: 2027, month: 1))
        #expect(YearMonth(year: 2026, month: 1).adding(months: -1) == YearMonth(year: 2025, month: 12))
        #expect(YearMonth(year: 2026, month: 5).adding(months: -17) == YearMonth(year: 2024, month: 12))
    }

    @Test func countsDaysIncludingLeapYears() {
        #expect(YearMonth(year: 2024, month: 2).numberOfDays == 29)
        #expect(YearMonth(year: 2026, month: 2).numberOfDays == 28)
        #expect(YearMonth(year: 2000, month: 2).numberOfDays == 29)
        #expect(YearMonth(year: 1900, month: 2).numberOfDays == 28)
        #expect(YearMonth(year: 2026, month: 4).numberOfDays == 30)
    }

    @Test(arguments: [
        (YearMonth(year: 2026, month: 10), 5), // Thursday
        (YearMonth(year: 2026, month: 2), 1),  // Sunday
        (YearMonth(year: 2025, month: 1), 4),  // Wednesday
        (YearMonth(year: 2024, month: 6), 7)   // Saturday
    ])
    func firstWeekdayMatchesCalendar(_ month: YearMonth, _ expected: Int) {
        #expect(month.firstWeekday == expected)
    }
}
