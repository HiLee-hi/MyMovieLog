import Domain

/// In-progress record form values. Held by ViewModels, not by the form view, so a draft
/// survives the sheet being re-created (rotation, iPad multitasking resizes).
public struct RecordDraft: Equatable, Sendable {
    public var status: WatchStatus
    public var rating: Double
    public var watchedAt: LocalDate?
    public var review: String
    public var memo: String

    public init(
        status: WatchStatus = .watched,
        rating: Double = 0,
        watchedAt: LocalDate? = nil,
        review: String = "",
        memo: String = ""
    ) {
        self.status = status
        self.rating = rating
        self.watchedAt = watchedAt
        self.review = review
        self.memo = memo
    }

    public init(record: MovieRecord?) {
        self.init(
            status: record?.status ?? .watched,
            rating: record?.rating ?? 0,
            watchedAt: record?.watchedAt,
            review: record?.review ?? "",
            memo: record?.memo ?? ""
        )
    }

    /// Writes the form into a record the same way the Android editor does: a watched record
    /// keeps rating, date and review; a wishlist record keeps only the memo. Blank text and a
    /// zero rating are stored as null.
    public func applied(to record: MovieRecord) -> MovieRecord {
        var updated = record
        let watched = status == .watched
        updated.status = status
        updated.rating = watched && rating > 0 ? rating : nil
        updated.watchedAt = watched ? watchedAt : nil
        updated.review = watched ? review.nonBlank : nil
        updated.memo = watched ? nil : memo.nonBlank
        return updated
    }
}

public enum SaveState: Equatable, Sendable {
    case idle
    case saving
    case success
    case error(String)

    public var isSaving: Bool { self == .saving }

    public var errorMessage: String? {
        if case .error(let message) = self { return message }
        return nil
    }
}

extension String {
    var nonBlank: String? {
        trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : self
    }
}
