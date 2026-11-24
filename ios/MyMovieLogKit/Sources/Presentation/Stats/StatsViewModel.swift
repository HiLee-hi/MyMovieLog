import Domain
import Foundation
import Observation

/// Watched count of one month in the "last 6 months" chart
public struct MonthCount: Equatable, Sendable {
    public let month: YearMonth
    public let count: Int
}

/// This month against last month, for the "지난달보다 3편 더" line
public enum MonthComparison: Equatable, Sendable {
    /// Both months are empty, e.g. right after signing up: nothing is shown
    case none
    case more(Int)
    case fewer(Int)
    case same
}

public struct StatsUiState: Equatable, Sendable {
    public var totalWatched = 0
    public var wishlistCount = 0
    /// nil while no watched record has a rating
    public var averageRating: Double?
    public var thisMonthCount = 0
    public var lastMonthCount = 0
    /// Oldest first, ending with the current month
    public var monthlyCounts: [MonthCount] = []

    public init() {}

    public init(records: [MovieRecord], currentMonth: YearMonth, monthsShown: Int = 6) {
        let watched = records.filter { $0.status == .watched }
        totalWatched = watched.count
        wishlistCount = records.count { $0.status == .wishlist }

        let ratings = watched.compactMap(\.rating)
        averageRating = ratings.isEmpty ? nil : ratings.reduce(0, +) / Double(ratings.count)

        func count(in month: YearMonth) -> Int {
            watched.count { record in record.watchedAt.map(month.contains) ?? false }
        }
        monthlyCounts = (0..<monthsShown).reversed().map { offset in
            let month = currentMonth.adding(months: -offset)
            return MonthCount(month: month, count: count(in: month))
        }
        thisMonthCount = count(in: currentMonth)
        lastMonthCount = count(in: currentMonth.adding(months: -1))
    }

    public var comparison: MonthComparison {
        if thisMonthCount == 0 && lastMonthCount == 0 { return .none }
        let diff = thisMonthCount - lastMonthCount
        if diff > 0 { return .more(diff) }
        if diff < 0 { return .fewer(-diff) }
        return .same
    }

    /// "4.3", one decimal like the Android app; "-" without ratings
    public var averageRatingText: String {
        averageRating.map { String(format: "%.1f", $0) } ?? "-"
    }

    /// Largest monthly count, never 0 so bar heights can be divided by it
    public var chartMaximum: Int {
        max(monthlyCounts.map(\.count).max() ?? 0, 1)
    }
}

@MainActor
@Observable
public final class StatsViewModel {
    public private(set) var uiState = StatsUiState()

    private let getRecords: GetRecordsUseCase

    public init(getRecords: GetRecordsUseCase) {
        self.getRecords = getRecords
    }

    public func observeRecords() async {
        for await records in getRecords() {
            uiState = StatsUiState(records: records, currentMonth: .current())
        }
    }
}
