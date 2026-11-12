import Domain
@testable import Presentation
import Testing

struct RecordDraftTests {
    private let base = MovieRecord(
        id: "r1", userId: "u", tmdbId: 1, title: "t", status: .watched,
        rating: 3, review: "old", memo: "old memo",
        watchedAt: LocalDate(year: 2026, month: 1, day: 1), photoUrls: ["p.jpg"]
    )

    @Test func watchedKeepsRatingDateAndReview() {
        let draft = RecordDraft(status: .watched, rating: 4.5, watchedAt: LocalDate(year: 2026, month: 10, day: 3),
                                review: "좋았다", memo: "ignored")
        let record = draft.applied(to: base)
        #expect(record.rating == 4.5)
        #expect(record.watchedAt == LocalDate(year: 2026, month: 10, day: 3))
        #expect(record.review == "좋았다")
        #expect(record.memo == nil)
        #expect(record.photoUrls == ["p.jpg"])
        #expect(record.id == "r1")
    }

    @Test func zeroRatingAndBlankTextBecomeNil() {
        let record = RecordDraft(status: .watched, rating: 0, review: "  \n").applied(to: base)
        #expect(record.rating == nil)
        #expect(record.review == nil)
        #expect(record.watchedAt == nil)
    }

    @Test func wishlistKeepsOnlyMemo() {
        let record = RecordDraft(status: .wishlist, rating: 5, watchedAt: .today(), review: "r", memo: "보고 싶다")
            .applied(to: base)
        #expect(record.status == .wishlist)
        #expect(record.rating == nil)
        #expect(record.watchedAt == nil)
        #expect(record.review == nil)
        #expect(record.memo == "보고 싶다")
    }

    @Test func roundTripsFromRecord() {
        let draft = RecordDraft(record: base)
        #expect(draft.rating == 3)
        #expect(draft.review == "old")
        #expect(RecordDraft(record: nil) == RecordDraft())
    }
}

struct HomeUiStateTests {
    @Test func summarizesRecords() {
        let october = YearMonth(year: 2026, month: 10)
        var records = (1...7).map { index in
            MovieRecord(id: "w\(index)", tmdbId: index, title: "w\(index)", status: .watched,
                        watchedAt: index <= 3 ? october.day(index) : LocalDate(year: 2026, month: 9, day: index))
        }
        records.append(MovieRecord(id: "x", tmdbId: 99, title: "x", status: .wishlist))
        records.append(MovieRecord(id: "y", tmdbId: 98, title: "y", status: .watched))

        let state = HomeUiState(records: records, currentMonth: october)
        #expect(state.totalWatched == 8)
        #expect(state.thisMonthCount == 3)
        #expect(state.recentWatched.map(\.id) == ["w1", "w2", "w3", "w4", "w5"])
        #expect(state.wishlistPreview.map(\.id) == ["x"])
    }
}
