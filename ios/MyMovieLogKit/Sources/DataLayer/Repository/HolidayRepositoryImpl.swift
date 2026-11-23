import Domain
import Foundation

/// Cache first; a month is requested once and kept. Errors are logged and show as "no holidays",
/// without caching, so the next visit to the month tries again (same as the Android app).
struct HolidayRepositoryImpl: HolidayRepository {
    private let api: HolidayApi?
    private let cache: HolidayCache

    /// `api == nil` when PUBLIC_DATA_API_KEY is not set: the calendar then only colors weekends
    init(api: HolidayApi?, cache: HolidayCache) {
        self.api = api
        self.cache = cache
    }

    func holidays(in month: YearMonth) async -> Set<LocalDate> {
        if let cached = await cache.holidays(in: month) {
            return cached
        }
        guard let api else { return [] }
        do {
            let items = try await api.holidayItems(year: month.year, month: month.month)
            let dates = Set(items.compactMap { LocalDate(basicIsoNumber: $0.locdate) })
            await cache.store(dates, in: month)
            Log.holiday.info("Fetched \(dates.count) holidays for \(month.year)-\(month.month)")
            return dates
        } catch {
            Log.holiday.error("Holiday fetch failed for \(month.year)-\(month.month): \(String(describing: error), privacy: .public)")
            return []
        }
    }
}

extension LocalDate {
    /// 20260924 → 2026-09-24
    init?(basicIsoNumber value: Int) {
        let year = value / 10_000, month = value / 100 % 100, day = value % 100
        guard (1...12).contains(month), (1...31).contains(day) else { return nil }
        self.init(year: year, month: month, day: day)
    }
}
