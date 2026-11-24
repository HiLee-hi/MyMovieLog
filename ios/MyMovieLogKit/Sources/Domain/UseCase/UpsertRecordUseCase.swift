import Foundation

// upload new photos -> save record -> delete removed photos (same order as android)
public struct UpsertRecordUseCase: Sendable {
    private let repository: any MovieRecordRepository
    private let photoRepository: (any PhotoRepository)?

    // no photoRepository -> photos untouched (previews / tests)
    public init(repository: any MovieRecordRepository, photoRepository: (any PhotoRepository)? = nil) {
        self.repository = repository
        self.photoRepository = photoRepository
    }

    // record.photoUrls = keep, newPhotos appended after upload
    // save fails -> roll back uploads, removed photo delete fails -> orphan, save still ok
    // returns uploaded paths in newPhotos order
    @discardableResult
    public func callAsFunction(_ record: MovieRecord, newPhotos: [Data] = []) async throws -> [String] {
        guard let photoRepository else {
            try await repository.upsertRecord(record)
            return []
        }
        let previousPhotos = await currentPhotoPaths(tmdbId: record.tmdbId)

        var saved = record
        let uploaded = newPhotos.isEmpty
            ? []
            : try await photoRepository.upload(newPhotos, userId: record.userId, tmdbId: record.tmdbId)
        saved.photoUrls += uploaded

        do {
            try await repository.upsertRecord(saved)
        } catch {
            if !uploaded.isEmpty { try? await photoRepository.delete(paths: uploaded) }
            throw error
        }

        let kept = Set(saved.photoUrls)
        let removed = previousPhotos.filter { !kept.contains($0) }
        if !removed.isEmpty {
            try? await photoRepository.delete(paths: removed)
        }
        return uploaded
    }

    private func currentPhotoPaths(tmdbId: Int) async -> [String] {
        var iterator = repository.getRecordByTmdbId(tmdbId).makeAsyncIterator()
        return (await iterator.next() ?? nil)?.photoUrls ?? []
    }
}
