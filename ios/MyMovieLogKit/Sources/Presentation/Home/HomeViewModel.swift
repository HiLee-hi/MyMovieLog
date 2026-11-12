import Domain
import Observation

public struct HomeUiState: Equatable, Sendable {
    public var recentWatched: [MovieRecord] = []
    public var wishlistPreview: [MovieRecord] = []
    public var totalWatched = 0
    public var thisMonthCount = 0

    public init() {}

    /// `records` are newest first, as the repository emits them
    public init(records: [MovieRecord], currentMonth: YearMonth) {
        let watched = records.filter { $0.status == .watched }
        let wishlist = records.filter { $0.status == .wishlist }
        recentWatched = Array(watched.prefix(5))
        wishlistPreview = Array(wishlist.prefix(5))
        totalWatched = watched.count
        thisMonthCount = watched.filter { record in
            record.watchedAt.map(currentMonth.contains) ?? false
        }.count
    }
}

@MainActor
@Observable
public final class HomeViewModel {
    public private(set) var uiState = HomeUiState()
    public let editSession: RecordEditSession

    private let getRecords: GetRecordsUseCase

    public init(getRecords: GetRecordsUseCase, upsertRecord: UpsertRecordUseCase) {
        self.getRecords = getRecords
        editSession = RecordEditSession(upsertRecord: upsertRecord)
    }

    public func observeRecords() async {
        for await records in getRecords() {
            uiState = HomeUiState(records: records, currentMonth: .current())
        }
    }
}
