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

    public init(upsertRecord: UpsertRecordUseCase) {
        self.upsertRecord = upsertRecord
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

    /// Photos attached in the Android app are kept as they are
    public func save() async {
        guard let record, !saveState.isSaving else { return }
        saveState = .saving
        do {
            try await upsertRecord(draft.applied(to: record))
            saveState = .success
        } catch {
            saveState = .error(error.localizedDescription)
        }
    }
}
