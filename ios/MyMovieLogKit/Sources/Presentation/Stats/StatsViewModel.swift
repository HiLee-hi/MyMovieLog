import Domain
import Foundation
import Observation

// one month in the last 6 months chart
public struct MonthCount: Equatable, Sendable {
    public let month: YearMonth
    public let count: Int
}

// for the "지난달보다 3편 더" line
public enum MonthComparison: Equatable, Sendable {
    // both months empty (e.g. just signed up) -> hidden
    case none
    case more(Int)
    case fewer(Int)
    case same
}

public struct StatsUiState: Equatable, Sendable {
    public var totalWatched = 0
    public var wishlistCount = 0
    // nil until a watched record has a rating
    public var averageRating: Double?
    public var thisMonthCount = 0
    public var lastMonthCount = 0
    // oldest first, ends with the current month
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

    // "4.3", one decimal like android, "-" without ratings
    public var averageRatingText: String {
        averageRating.map { String(format: "%.1f", $0) } ?? "-"
    }

    // largest monthly count, min 1 so bar heights can divide by it
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
