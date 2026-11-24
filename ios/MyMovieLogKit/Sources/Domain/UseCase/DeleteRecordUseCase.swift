// record first, then Storage photos, a failed photo delete just leaves an orphan (same as android)
public struct DeleteRecordUseCase: Sendable {
    private let repository: any MovieRecordRepository
    private let photoRepository: (any PhotoRepository)?

    public init(repository: any MovieRecordRepository, photoRepository: (any PhotoRepository)? = nil) {
        self.repository = repository
        self.photoRepository = photoRepository
    }

    public func callAsFunction(recordId: String) async throws {
        var photos: [String] = []
        if photoRepository != nil {
            var iterator = repository.getRecords(status: nil).makeAsyncIterator()
            photos = (await iterator.next() ?? []).first { $0.id == recordId }?.photoUrls ?? []
        }
        try await repository.deleteRecord(id: recordId)
        if let photoRepository, !photos.isEmpty {
            try? await photoRepository.delete(paths: photos)
        }
    }
}
