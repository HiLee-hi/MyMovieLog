import Domain
import Observation

@MainActor
@Observable
public final class LibraryViewModel {
    public private(set) var watchedRecords: [MovieRecord] = []
    public private(set) var wishlistRecords: [MovieRecord] = []
    public private(set) var deleteError: String?
    public let editSession: RecordEditSession

    private let getRecords: GetRecordsUseCase
    private let deleteRecordUseCase: DeleteRecordUseCase

    public init(
        getRecords: GetRecordsUseCase,
        upsertRecord: UpsertRecordUseCase,
        deleteRecord: DeleteRecordUseCase
    ) {
        self.getRecords = getRecords
        self.deleteRecordUseCase = deleteRecord
        editSession = RecordEditSession(upsertRecord: upsertRecord)
    }

    public func records(for status: WatchStatus) -> [MovieRecord] {
        status == .watched ? watchedRecords : wishlistRecords
    }

    public func observeRecords() async {
        // One stream split in two keeps both tabs consistent with each other
        for await records in getRecords() {
            watchedRecords = records.filter { $0.status == .watched }
            wishlistRecords = records.filter { $0.status == .wishlist }
        }
    }

    public func deleteRecord(_ record: MovieRecord) async {
        do {
            try await deleteRecordUseCase(recordId: record.id)
            if editSession.record?.id == record.id { editSession.clear() }
        } catch {
            deleteError = error.localizedDescription
        }
    }

    public func clearDeleteError() {
        deleteError = nil
    }
}
