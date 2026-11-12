import Foundation

public struct Movie: Identifiable, Hashable, Sendable {
    public let id: Int
    public let title: String
    public let originalTitle: String
    public let overview: String
    public let posterPath: String?
    public let backdropPath: String?
    public let releaseDate: String
    public let voteAverage: Double
    public let genreIds: [Int]
    /// Populated only by the movie detail request, empty for search results
    public let genres: [String]

    public init(
        id: Int,
        title: String,
        originalTitle: String,
        overview: String,
        posterPath: String?,
        backdropPath: String?,
        releaseDate: String,
        voteAverage: Double,
        genreIds: [Int],
        genres: [String] = []
    ) {
        self.id = id
        self.title = title
        self.originalTitle = originalTitle
        self.overview = overview
        self.posterPath = posterPath
        self.backdropPath = backdropPath
        self.releaseDate = releaseDate
        self.voteAverage = voteAverage
        self.genreIds = genreIds
        self.genres = genres
    }

    public var posterURL: URL? { TmdbImage.url(path: posterPath, size: "w500") }

    public var backdropURL: URL? { TmdbImage.url(path: backdropPath, size: "w780") }

    /// "2024" from "2024-05-01"; nil when TMDB has no release date
    public var releaseYear: String? {
        releaseDate.count >= 4 ? String(releaseDate.prefix(4)) : nil
    }
}

/// One page of TMDB search results
public struct MoviePage: Sendable {
    public let movies: [Movie]
    public let page: Int
    public let totalPages: Int

    public init(movies: [Movie], page: Int, totalPages: Int) {
        self.movies = movies
        self.page = page
        self.totalPages = totalPages
    }

    public var hasNextPage: Bool { page < totalPages }
}

public enum TmdbImage {
    public static func url(path: String?, size: String) -> URL? {
        guard let path, !path.isEmpty else { return nil }
        return URL(string: "https://image.tmdb.org/t/p/\(size)\(path)")
    }
}
