import Foundation

public struct MovieRecord: Identifiable, Hashable, Sendable {
    /// Assigned by Supabase; empty until the record is first saved
    public var id: String
    public var userId: String
    public var tmdbId: Int
    public var title: String
    public var originalTitle: String
    public var posterPath: String?
    public var genreIds: [Int]
    public var status: WatchStatus
    /// 0.5 ~ 5.0
    public var rating: Double?
    public var review: String?
    public var memo: String?
    public var watchedAt: LocalDate?
    /// Storage paths in the `movie-photos` bucket, attached from the Android app
    public var photoUrls: [String]
    public var createdAt: String

    public init(
        id: String = "",
        userId: String = "",
        tmdbId: Int,
        title: String,
        originalTitle: String = "",
        posterPath: String? = nil,
        genreIds: [Int] = [],
        status: WatchStatus,
        rating: Double? = nil,
        review: String? = nil,
        memo: String? = nil,
        watchedAt: LocalDate? = nil,
        photoUrls: [String] = [],
        createdAt: String = ""
    ) {
        self.id = id
        self.userId = userId
        self.tmdbId = tmdbId
        self.title = title
        self.originalTitle = originalTitle
        self.posterPath = posterPath
        self.genreIds = genreIds
        self.status = status
        self.rating = rating
        self.review = review
        self.memo = memo
        self.watchedAt = watchedAt
        self.photoUrls = photoUrls
        self.createdAt = createdAt
    }

    public var posterURL: URL? { TmdbImage.url(path: posterPath, size: "w500") }
}

public enum WatchStatus: String, CaseIterable, Sendable {
    case watched
    case wishlist

    public static func from(_ value: String) -> WatchStatus {
        WatchStatus(rawValue: value) ?? .wishlist
    }
}
