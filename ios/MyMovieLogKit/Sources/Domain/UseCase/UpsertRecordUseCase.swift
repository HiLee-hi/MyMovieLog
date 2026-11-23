import Foundation

/// Saves a record together with its photos, in the same order as the Android app:
/// upload the new photos, save the record, then delete the photos the edit removed.
public struct UpsertRecordUseCase: Sendable {
    private let repository: any MovieRecordRepository
    private let photoRepository: (any PhotoRepository)?

    /// Without `photoRepository` photos are left untouched (previews and tests that do not need them)
    public init(repository: any MovieRecordRepository, photoRepository: (any PhotoRepository)? = nil) {
        self.repository = repository
        self.photoRepository = photoRepository
    }

    /// `record.photoUrls` are the photos to keep; `newPhotos` are appended after uploading.
    /// Removed photos are deleted only after the record no longer references them, and a failed
    /// delete never fails the save. If the save fails, the photos just uploaded are deleted again.
    /// Returns the storage paths of the uploaded photos, in the order of `newPhotos`.
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
