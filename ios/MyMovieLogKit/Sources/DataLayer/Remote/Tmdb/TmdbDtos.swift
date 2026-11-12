import Foundation

struct SearchResponseDto: Decodable {
    let page: Int
    let results: [MovieDto]
    let totalPages: Int

    enum CodingKeys: String, CodingKey {
        case page, results
        case totalPages = "total_pages"
    }
}

/// TMDB omits or nulls fields for obscure titles (release_date in particular), so every field
/// except the id falls back to an empty value instead of failing the whole page.
struct MovieDto: Decodable {
    let id: Int
    let title: String
    let originalTitle: String
    let overview: String
    let posterPath: String?
    let backdropPath: String?
    let releaseDate: String
    let voteAverage: Double
    let genreIds: [Int]

    enum CodingKeys: String, CodingKey {
        case id, title, overview
        case originalTitle = "original_title"
        case posterPath = "poster_path"
        case backdropPath = "backdrop_path"
        case releaseDate = "release_date"
        case voteAverage = "vote_average"
        case genreIds = "genre_ids"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        title = try c.decodeIfPresent(String.self, forKey: .title) ?? ""
        originalTitle = try c.decodeIfPresent(String.self, forKey: .originalTitle) ?? ""
        overview = try c.decodeIfPresent(String.self, forKey: .overview) ?? ""
        posterPath = try c.decodeIfPresent(String.self, forKey: .posterPath)
        backdropPath = try c.decodeIfPresent(String.self, forKey: .backdropPath)
        releaseDate = try c.decodeIfPresent(String.self, forKey: .releaseDate) ?? ""
        voteAverage = try c.decodeIfPresent(Double.self, forKey: .voteAverage) ?? 0
        genreIds = try c.decodeIfPresent([Int].self, forKey: .genreIds) ?? []
    }
}

struct MovieDetailDto: Decodable {
    let id: Int
    let title: String
    let originalTitle: String
    let overview: String
    let posterPath: String?
    let backdropPath: String?
    let releaseDate: String
    let voteAverage: Double
    let genres: [GenreDto]

    enum CodingKeys: String, CodingKey {
        case id, title, overview, genres
        case originalTitle = "original_title"
        case posterPath = "poster_path"
        case backdropPath = "backdrop_path"
        case releaseDate = "release_date"
        case voteAverage = "vote_average"
    }

    init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        id = try c.decode(Int.self, forKey: .id)
        title = try c.decodeIfPresent(String.self, forKey: .title) ?? ""
        originalTitle = try c.decodeIfPresent(String.self, forKey: .originalTitle) ?? ""
        overview = try c.decodeIfPresent(String.self, forKey: .overview) ?? ""
        posterPath = try c.decodeIfPresent(String.self, forKey: .posterPath)
        backdropPath = try c.decodeIfPresent(String.self, forKey: .backdropPath)
        releaseDate = try c.decodeIfPresent(String.self, forKey: .releaseDate) ?? ""
        voteAverage = try c.decodeIfPresent(Double.self, forKey: .voteAverage) ?? 0
        genres = try c.decodeIfPresent([GenreDto].self, forKey: .genres) ?? []
    }
}

struct GenreDto: Decodable {
    let id: Int
    let name: String
}
