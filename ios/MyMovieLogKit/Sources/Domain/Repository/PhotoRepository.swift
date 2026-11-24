import Foundation

// private bucket `movie-photos`, records keep paths (`{user_id}/{tmdb_id}/{uuid}.jpg`)
// viewing needs signed URLs
public protocol PhotoRepository: Sendable {
    // by path, unsignable paths left out
    func signedURLs(for paths: [String]) async throws -> [String: URL]

    // resize + compress, upload under `{userId}/{tmdbId}/`, paths in input order
    func upload(_ images: [Data], userId: String, tmdbId: Int) async throws -> [String]

    func delete(paths: [String]) async throws
}
