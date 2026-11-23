import Domain
import Foundation

/// Holidays already fetched, by month, in a JSON file in Application Support — the counterpart of
/// the Android `holidays` and `holiday_fetch` tables. A month without holidays is stored as an
/// empty list so it is not requested again. Public data, so it survives sign-out.
actor HolidayCache {
    private let fileURL: URL?
    /// "2026-09" → ["2026-09-24", ...]
    private var months: [String: [String]] = [:]
    private var isLoaded = false

    /// `fileURL == nil` keeps the cache in memory only (tests)
    init(fileURL: URL?) {
        self.fileURL = fileURL
    }

    static func defaultFileURL() -> URL? {
        FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first?
            .appending(path: "holidays.json")
    }

    /// nil when the month was never fetched
    func holidays(in month: YearMonth) -> Set<LocalDate>? {
        loadIfNeeded()
        return months[Self.key(month)].map { Set($0.compactMap(LocalDate.init(isoString:))) }
    }

    func store(_ holidays: Set<LocalDate>, in month: YearMonth) {
        loadIfNeeded()
        months[Self.key(month)] = holidays.map(\.isoString).sorted()
        persist()
    }

    private static func key(_ month: YearMonth) -> String {
        String(format: "%04d-%02d", month.year, month.month)
    }

    private func loadIfNeeded() {
        guard !isLoaded else { return }
        isLoaded = true
        guard let fileURL, let data = try? Data(contentsOf: fileURL),
              let stored = try? JSONDecoder().decode([String: [String]].self, from: data)
        else { return }
        months = stored
    }

    private func persist() {
        guard let fileURL else { return }
        do {
            try FileManager.default.createDirectory(at: fileURL.deletingLastPathComponent(), withIntermediateDirectories: true)
            try JSONEncoder().encode(months).write(to: fileURL, options: .atomic)
        } catch {
            Log.holiday.error("Holiday cache write failed: \(error.localizedDescription, privacy: .public)")
        }
    }
}
