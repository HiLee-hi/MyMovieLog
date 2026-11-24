import Foundation

// library item per uploaded photo, device-only like android photoSourceUris
// lets the editor skip album photos already attached
@MainActor
public final class PhotoSourceIndex {
    public static let shared = PhotoSourceIndex(defaults: .standard)

    private static let key = "photoSourceIndex"
    private let defaults: UserDefaults
    // storage path -> library item id
    private var sources: [String: String]

    public init(defaults: UserDefaults) {
        self.defaults = defaults
        sources = defaults.dictionary(forKey: Self.key) as? [String: String] ?? [:]
    }

    public func sourceIds(for paths: [String]) -> Set<String> {
        Set(paths.compactMap { sources[$0] })
    }

    // sourceIds match paths, camera photos (no item) skipped
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
