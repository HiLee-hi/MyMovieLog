@testable import DataLayer
import Domain
import Foundation
import Testing

struct TmdbDtoTests {
    @Test func decodesSearchResultWithMissingFields() throws {
        let json = """
        {"page":1,"total_pages":3,"total_results":41,"results":[
          {"id":7,"title":"제목","original_title":"Title","overview":"","poster_path":null,
           "vote_average":7.1,"genre_ids":[18]},
          {"id":8,"title":"No date","release_date":null}
        ]}
        """
        let response = try JSONDecoder().decode(SearchResponseDto.self, from: Data(json.utf8))
        #expect(response.totalPages == 3)
        #expect(response.results.map(\.id) == [7, 8])
        #expect(response.results[0].releaseDate == "")
        #expect(response.results[1].genreIds.isEmpty)
        #expect(response.results[1].toDomain().releaseYear == nil)
    }

    @Test func mapsDetailGenres() throws {
        let json = """
        {"id":496243,"title":"기생충","original_title":"기생충","overview":"o","poster_path":"/p.jpg",
         "backdrop_path":"/b.jpg","release_date":"2019-05-30","vote_average":8.5,
         "genres":[{"id":35,"name":"코미디"},{"id":18,"name":"드라마"}]}
        """
        let movie = try JSONDecoder().decode(MovieDetailDto.self, from: Data(json.utf8)).toDomain()
        #expect(movie.genreIds == [35, 18])
        #expect(movie.genres == ["코미디", "드라마"])
    }
}

struct MovieRecordDtoTests {
    private func encodedObject(_ dto: MovieRecordDto) throws -> [String: Any] {
        let data = try JSONEncoder().encode(dto)
        return try #require(JSONSerialization.jsonObject(with: data) as? [String: Any])
    }

    @Test func newRecordOmitsServerAssignedColumns() throws {
        let record = MovieRecord(userId: "u", tmdbId: 1, title: "t", status: .wishlist, memo: "m")
        let object = try encodedObject(record.toDto())
        #expect(object["id"] == nil)
        #expect(object["created_at"] == nil)
        #expect(object["status"] as? String == "wishlist")
        // Cleared fields are sent as null so an update really clears them
        #expect(object["rating"] is NSNull)
        #expect(object["watched_at"] is NSNull)
    }

    @Test func existingRecordKeepsIdAndDate() throws {
        let record = MovieRecord(id: "abc", userId: "u", tmdbId: 1, title: "t", status: .watched,
                                 rating: 4.5, watchedAt: LocalDate(year: 2026, month: 10, day: 3),
                                 photoUrls: ["u/1/a.jpg"], createdAt: "2026-10-03T10:00:00Z")
        let object = try encodedObject(record.toDto())
        #expect(object["id"] as? String == "abc")
        #expect(object["watched_at"] as? String == "2026-10-03")
        #expect(object["rating"] as? Double == 4.5)
        #expect(object["photo_urls"] as? [String] == ["u/1/a.jpg"])
    }

    @Test func decodesSupabaseRow() throws {
        let json = """
        {"id":"abc","user_id":"u","tmdb_id":1,"title":"t","original_title":"o","poster_path":null,
         "genre_ids":[1,2],"status":"watched","rating":3.5,"review":null,"memo":null,
         "watched_at":"2026-10-03","photo_urls":[],"created_at":"2026-10-03T10:00:00.123+00:00"}
        """
        let record = try JSONDecoder().decode(MovieRecordDto.self, from: Data(json.utf8)).toDomain()
        #expect(record.id == "abc")
        #expect(record.status == .watched)
        #expect(record.rating == 3.5)
        #expect(record.watchedAt == LocalDate(year: 2026, month: 10, day: 3))
    }
}
