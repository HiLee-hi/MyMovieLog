import Foundation

/// Record photos in the private Supabase Storage bucket `movie-photos`.
/// A record keeps storage paths (`{user_id}/{tmdb_id}/{uuid}.jpg`); viewing needs signed URLs.
public protocol PhotoRepository: Sendable {
    /// Temporary URLs by path. Paths the server cannot sign are left out.
    func signedURLs(for paths: [String]) async throws -> [String: URL]

    /// Resizes and compresses each image (JPEG/HEIC/PNG data from the picker or camera), uploads
    /// it under `{userId}/{tmdbId}/` and returns the new storage paths in the same order.
    func upload(_ images: [Data], userId: String, tmdbId: Int) async throws -> [String]

    func delete(paths: [String]) async throws
}
