import Foundation

public struct GetSignedPhotoUrlsUseCase: Sendable {
    private let repository: any PhotoRepository

    public init(repository: any PhotoRepository) {
        self.repository = repository
    }

    public func callAsFunction(paths: [String]) async throws -> [String: URL] {
        guard !paths.isEmpty else { return [:] }
        return try await repository.signedURLs(for: paths)
    }
}
