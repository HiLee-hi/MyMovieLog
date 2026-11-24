import Foundation

/// Row of the `movie_records` table. Also the on-disk format of the local cache.
struct MovieRecordDto: Codable, Equatable, Sendable {
    var id: String?
    var userId: String
    var tmdbId: Int
    var title: String
    var originalTitle: String
    var posterPath: String?
    var genreIds: [Int]
    var status: String
    var rating: Double?
    var review: String?
    var memo: String?
    var watchedAt: String?
    var photoUrls: [String]
    var createdAt: String?

    enum CodingKeys: String, CodingKey {
        case id, title, status, rating, review, memo
        case userId = "user_id"
        case tmdbId = "tmdb_id"
        case originalTitle = "original_title"
        case posterPath = "poster_path"
        case genreIds = "genre_ids"
        case watchedAt = "watched_at"
        case photoUrls = "photo_urls"
        case createdAt = "created_at"
    }

    init(
        id: String?, userId: String, tmdbId: Int, title: String, originalTitle: String,
        posterPath: String?, genreIds: [Int], status: String, rating: Double?, review: String?,
        memo: String?, watchedAt: String?, photoUrls: [String], createdAt: String?
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

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decodeIfPresent(String.self, forKey: .id)
        userId = try c.decodeIfPresent(String.self, forKey: .userId) ?? ""
        tmdbId = try c.decode(Int.self, forKey: .tmdbId)
        title = try c.decode(String.self, forKey: .title)
        originalTitle = try c.decodeIfPresent(String.self, forKey: .originalTitle) ?? ""
        posterPath = try c.decodeIfPresent(String.self, forKey: .posterPath)
        genreIds = try c.decodeIfPresent([Int].self, forKey: .genreIds) ?? []
        status = try c.decode(String.self, forKey: .status)
        rating = try c.decodeIfPresent(Double.self, forKey: .rating)
        review = try c.decodeIfPresent(String.self, forKey: .review)
        memo = try c.decodeIfPresent(String.self, forKey: .memo)
        watchedAt = try c.decodeIfPresent(String.self, forKey: .watchedAt)
        photoUrls = try c.decodeIfPresent([String].self, forKey: .photoUrls) ?? []
        createdAt = try c.decodeIfPresent(String.self, forKey: .createdAt)
    }

    /// `id` and `created_at` are left out until the server has assigned them. The editable
    /// fields are always written, as null when empty, so clearing a rating or review in an
    /// update actually clears the column.
    func encode(to encoder: Encoder) throws {
        var c = encoder.container(keyedBy: CodingKeys.self)
        if let id, !id.isEmpty { try c.encode(id, forKey: .id) }
        try c.encode(userId, forKey: .userId)
        try c.encode(tmdbId, forKey: .tmdbId)
        try c.encode(title, forKey: .title)
        try c.encode(originalTitle, forKey: .originalTitle)
        try c.encode(posterPath, forKey: .posterPath)
        try c.encode(genreIds, forKey: .genreIds)
        try c.encode(status, forKey: .status)
        try c.encode(rating, forKey: .rating)
        try c.encode(review, forKey: .review)
        try c.encode(memo, forKey: .memo)
        try c.encode(watchedAt, forKey: .watchedAt)
        try c.encode(photoUrls, forKey: .photoUrls)
        if let createdAt, !createdAt.isEmpty { try c.encode(createdAt, forKey: .createdAt) }
    }
}
