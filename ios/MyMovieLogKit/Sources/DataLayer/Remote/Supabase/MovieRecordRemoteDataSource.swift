import Foundation
import Supabase

protocol MovieRecordRemoteDataSource: Sendable {
    func upsert(_ record: MovieRecordDto) async throws -> MovieRecordDto
    func delete(id: String) async throws
    /// Rows of the signed-in user (RLS limits the select to them)
    func fetchAll() async throws -> [MovieRecordDto]
}

struct SupabaseMovieRecordDataSource: MovieRecordRemoteDataSource {
    private static let table = "movie_records"

    let client: SupabaseClient

    func upsert(_ record: MovieRecordDto) async throws -> MovieRecordDto {
        try await client.from(Self.table)
            .upsert(record, onConflict: "user_id,tmdb_id")
            .select()
            .single()
            .execute()
            .value
    }

    func delete(id: String) async throws {
        try await client.from(Self.table)
            .delete()
            .eq("id", value: id)
            .execute()
    }

    func fetchAll() async throws -> [MovieRecordDto] {
        try await client.from(Self.table)
            .select()
            .execute()
            .value
    }
}
