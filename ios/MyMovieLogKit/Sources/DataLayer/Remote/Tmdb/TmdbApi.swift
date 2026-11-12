import Foundation

enum TmdbError: LocalizedError {
    case http(statusCode: Int)

    var errorDescription: String? {
        switch self {
        case .http(let statusCode): return "TMDB 요청에 실패했어요 (\(statusCode))"
        }
    }
}

struct TmdbApi: Sendable {
    private static let baseURL = URL(string: "https://api.themoviedb.org/3/")!

    private let apiKey: String
    private let session: URLSession

    init(apiKey: String, session: URLSession = .shared) {
        self.apiKey = apiKey
        self.session = session
    }

    func searchMovies(query: String, page: Int) async throws -> SearchResponseDto {
        try await get("search/movie", query: [
            URLQueryItem(name: "query", value: query),
            URLQueryItem(name: "page", value: String(page)),
            URLQueryItem(name: "include_adult", value: "false")
        ])
    }

    func getMovieDetail(movieId: Int) async throws -> MovieDetailDto {
        try await get("movie/\(movieId)", query: [])
    }

    private func get<T: Decodable>(_ path: String, query: [URLQueryItem]) async throws -> T {
        var components = URLComponents(url: Self.baseURL.appending(path: path), resolvingAgainstBaseURL: false)!
        components.queryItems = query + [URLQueryItem(name: "language", value: "ko-KR")]
        var request = URLRequest(url: components.url!)
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "accept")

        let (data, response) = try await session.data(for: request)
        if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
            throw TmdbError.http(statusCode: http.statusCode)
        }
        return try JSONDecoder().decode(T.self, from: data)
    }
}
