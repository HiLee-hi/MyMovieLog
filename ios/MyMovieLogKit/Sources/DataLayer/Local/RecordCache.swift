import Domain
import Foundation

/// Local copy of the signed-in user's records, the iOS counterpart of the Android Room cache.
/// Screens read from here so they work offline; every change is pushed to all observers and
/// written to a JSON file in Application Support.
actor RecordCache {
    private let fileURL: URL?
    private var records: [MovieRecord] = []
    private var isLoaded = false
    private var observers: [UUID: @Sendable ([MovieRecord]) -> Void] = [:]

    /// `fileURL == nil` keeps the cache in memory only (tests, previews)
    init(fileURL: URL?) {
        self.fileURL = fileURL
    }

    static func defaultFileURL() -> URL? {
        guard let directory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask).first
        else { return nil }
        return directory.appending(path: "movie_records.json")
    }

    /// Emits the transformed records right away and after every change.
    nonisolated func observe<T: Sendable>(
        _ transform: @escaping @Sendable ([MovieRecord]) -> T
    ) -> AsyncStream<T> {
        AsyncStream { continuation in
            let id = UUID()
            continuation.onTermination = { _ in
                Task { await self.removeObserver(id) }
            }
            Task {
                await self.addObserver(id) { records in
                    continuation.yield(transform(records))
                }
            }
        }
    }

    func allRecords() -> [MovieRecord] {
        loadIfNeeded()
        return records
    }

    func upsert(_ record: MovieRecord) {
        loadIfNeeded()
        records.removeAll { $0.id == record.id || $0.tmdbId == record.tmdbId }
        records.append(record)
        didChange()
    }

    func delete(id: String) {
        loadIfNeeded()
        records.removeAll { $0.id == id }
        didChange()
    }

    func replaceAll(_ newRecords: [MovieRecord]) {
        isLoaded = true
        records = newRecords
        didChange()
    }

    func clear() {
        replaceAll([])
    }

    private func addObserver(_ id: UUID, _ observer: @escaping @Sendable ([MovieRecord]) -> Void) {
        loadIfNeeded()
        observers[id] = observer
        observer(records)
    }

    private func removeObserver(_ id: UUID) {
        observers[id] = nil
    }

    private func didChange() {
        // Newest first, like `ORDER BY created_at DESC` in the Android DAO
        records.sort { $0.createdAt > $1.createdAt }
        persist()
        for observer in observers.values {
            observer(records)
        }
    }

    private func loadIfNeeded() {
        guard !isLoaded else { return }
        isLoaded = true
        guard let fileURL, let data = try? Data(contentsOf: fileURL),
              let dtos = try? JSONDecoder().decode([MovieRecordDto].self, from: data)
        else { return }
        records = dtos.map { $0.toDomain() }.sorted { $0.createdAt > $1.createdAt }
    }

    private func persist() {
        guard let fileURL else { return }
        do {
            try FileManager.default.createDirectory(
                at: fileURL.deletingLastPathComponent(),
                withIntermediateDirectories: true
            )
            let data = try JSONEncoder().encode(records.map { $0.toDto() })
            try data.write(to: fileURL, options: [.atomic, .completeFileProtection])
        } catch {
            Log.record.error("Cache write failed: \(error.localizedDescription, privacy: .public)")
        }
    }
}
