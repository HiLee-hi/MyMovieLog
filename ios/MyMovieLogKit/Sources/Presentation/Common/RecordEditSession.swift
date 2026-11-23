import Domain
import Observation

/// Editing an existing record from Home, Library or Calendar: the selected record, its draft
/// and the save state. One instance per screen.
@MainActor
@Observable
public final class RecordEditSession {
    public private(set) var record: MovieRecord?
    public var draft = RecordDraft()
    public private(set) var saveState: SaveState = .idle

    private let upsertRecord: UpsertRecordUseCase
    private let photoSources: PhotoSourceIndex

    public init(upsertRecord: UpsertRecordUseCase, photoSources: PhotoSourceIndex = .shared) {
        self.upsertRecord = upsertRecord
        self.photoSources = photoSources
    }

    public func select(_ record: MovieRecord) {
        self.record = record
        draft = RecordDraft(record: record)
        saveState = .idle
    }

    public func clear() {
        record = nil
        draft = RecordDraft()
        saveState = .idle
    }

    /// Uploads the new photos, saves, then deletes the photos that were removed
    public func save() async {
        guard let record, !saveState.isSaving else { return }
        saveState = .saving
        do {
            let uploaded = try await upsertRecord(draft.applied(to: record), newPhotos: draft.newPhotos.map(\.data))
            photoSources.remember(paths: uploaded, sourceIds: draft.newPhotos.map(\.sourceId))
            saveState = .success
        } catch {
            saveState = .error(error.localizedDescription)
        }
    }
}
