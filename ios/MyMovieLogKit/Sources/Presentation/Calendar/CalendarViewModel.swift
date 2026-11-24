import Domain
import Observation

@MainActor
@Observable
public final class CalendarViewModel {
    public private(set) var currentMonth: YearMonth
    // date shown in the sheet
    public var selectedDate: LocalDate?
    public private(set) var watchedDates: Set<LocalDate> = []
    // holidays of currentMonth, red like sundays
    public private(set) var holidays: Set<LocalDate> = []
    public private(set) var selectedDateRecords: [MovieRecord] = []
    public let editSession: RecordEditSession

    private let getWatchedDatesByMonth: GetWatchedDatesByMonthUseCase
    private let getRecordsByDate: GetRecordsByDateUseCase
    private let getHolidaysByMonth: GetHolidaysByMonthUseCase

    public init(
        getWatchedDatesByMonth: GetWatchedDatesByMonthUseCase,
        getRecordsByDate: GetRecordsByDateUseCase,
        getHolidaysByMonth: GetHolidaysByMonthUseCase,
        upsertRecord: UpsertRecordUseCase,
        initialMonth: YearMonth = .current()
    ) {
        self.getWatchedDatesByMonth = getWatchedDatesByMonth
        self.getRecordsByDate = getRecordsByDate
        self.getHolidaysByMonth = getHolidaysByMonth
        currentMonth = initialMonth
        editSession = RecordEditSession(upsertRecord: upsertRecord)
    }

    public func showPreviousMonth() {
        currentMonth = currentMonth.adding(months: -1)
    }

    public func showNextMonth() {
        currentMonth = currentMonth.adding(months: 1)
    }

    public func showToday() {
        currentMonth = .current()
    }

    public func selectDate(_ date: LocalDate) {
        selectedDate = date
    }

    public func dismissDate() {
        selectedDate = nil
        editSession.clear()
    }

    // restarted by the view on currentMonth change
    public func observeWatchedDates() async {
        for await dates in getWatchedDatesByMonth(month: currentMonth) {
            watchedDates = dates
        }
    }

    // restarted by the view on currentMonth change
    public func loadHolidays() async {
        let month = currentMonth
        let dates = await getHolidaysByMonth(month: month)
        // fast swipes can finish an older month's request last
        if month == currentMonth, !Task.isCancelled {
            holidays = dates
        }
    }

    // restarted by the view on selectedDate change
    public func observeSelectedDateRecords() async {
        guard let date = selectedDate else {
            selectedDateRecords = []
            return
        }
        for await records in getRecordsByDate(date: date) {
            selectedDateRecords = records
        }
    }
}
