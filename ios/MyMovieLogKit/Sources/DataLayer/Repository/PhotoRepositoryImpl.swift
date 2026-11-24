import Domain
import Foundation
import Supabase

/// Same bucket and URL lifetime as the Android `PhotoRepositoryImpl`
struct PhotoRepositoryImpl: PhotoRepository {
    static let bucket = "movie-photos"
    static let signedURLLifetime = 3600

    private let storage: SupabaseStorageClient

    init(storage: SupabaseStorageClient) {
        self.storage = storage
    }

    func signedURLs(for paths: [String]) async throws -> [String: URL] {
        // One request for all paths; the server answers per path
        let results = try await storage.from(Self.bucket)
            .createSignedURLs(paths: paths, expiresIn: Self.signedURLLifetime)
        var urls: [String: URL] = [:]
        for result in results {
            switch result {
            case .success(let path, let url):
                urls[path] = url
            case .failure(let path, let error):
                Log.photo.error("Signing failed for \(path, privacy: .private): \(error, privacy: .public)")
            }
        }
        return urls
    }

    func upload(_ images: [Data], userId: String, tmdbId: Int) async throws -> [String] {
        var paths: [String] = []
        do {
            for image in images {
                let jpeg = try ImageCompressor.jpegData(from: image)
                // Lowercase like the Android java.util.UUID paths
                let path = "\(userId)/\(tmdbId)/\(UUID().uuidString.lowercased()).jpg"
                try await storage.from(Self.bucket).upload(
                    path,
                    data: jpeg,
                    options: FileOptions(contentType: "image/jpeg", upsert: true)
                )
                Log.photo.info("Uploaded photo (\(jpeg.count / 1024, privacy: .public) KB)")
                paths.append(path)
            }
        } catch {
            // All or nothing: a half-finished batch would leave files no record points to
            if !paths.isEmpty { try? await delete(paths: paths) }
            throw error
        }
        return paths
    }

    func delete(paths: [String]) async throws {
        guard !paths.isEmpty else { return }
        _ = try await storage.from(Self.bucket).remove(paths: paths)
        Log.photo.info("Deleted \(paths.count, privacy: .public) photos")
    }
}
