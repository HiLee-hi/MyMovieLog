import Foundation

/// Which photo library item each uploaded photo came from. Like the Android app's local-only
/// photoSourceUris it stays on this device, and lets the editor skip an album photo that is
/// already attached to the record.
@MainActor
public final class PhotoSourceIndex {
    public static let shared = PhotoSourceIndex(defaults: .standard)

    private static let key = "photoSourceIndex"
    private let defaults: UserDefaults
    /// Storage path → photo library item identifier
    private var sources: [String: String]

    public init(defaults: UserDefaults) {
        self.defaults = defaults
        sources = defaults.dictionary(forKey: Self.key) as? [String: String] ?? [:]
    }

    public func sourceIds(for paths: [String]) -> Set<String> {
        Set(paths.compactMap { sources[$0] })
    }

    /// `sourceIds` line up with `paths`; photos without a library item (camera) are skipped
    public func remember(paths: [String], sourceIds: [String?]) {
        var changed = false
        for (path, sourceId) in zip(paths, sourceIds) {
            guard let sourceId else { continue }
            sources[path] = sourceId
            changed = true
        }
        if changed { defaults.set(sources, forKey: Self.key) }
    }
}
