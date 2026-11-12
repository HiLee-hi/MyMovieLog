import Foundation

/// API keys and endpoints. The app reads them from Info.plist, which gets them from
/// `Config/Secrets.xcconfig` (generated from the Android `local.properties`).
public struct AppConfig: Sendable {
    public let tmdbApiKey: String
    public let supabaseURL: URL
    public let supabaseAnonKey: String

    public init(tmdbApiKey: String, supabaseURL: URL, supabaseAnonKey: String) {
        self.tmdbApiKey = tmdbApiKey
        self.supabaseURL = supabaseURL
        self.supabaseAnonKey = supabaseAnonKey
    }

    /// Nil when a key is missing or still the `$(...)` placeholder of an unset build setting.
    public static func fromInfoDictionary(_ info: [String: Any]) -> AppConfig? {
        func value(_ key: String) -> String? {
            guard let raw = info[key] as? String else { return nil }
            let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            return trimmed.isEmpty || trimmed.hasPrefix("$(") ? nil : trimmed
        }
        guard let tmdbApiKey = value("TMDB_API_KEY"),
              let urlString = value("SUPABASE_URL"), let supabaseURL = URL(string: urlString),
              let supabaseAnonKey = value("SUPABASE_ANON_KEY")
        else { return nil }
        return AppConfig(tmdbApiKey: tmdbApiKey, supabaseURL: supabaseURL, supabaseAnonKey: supabaseAnonKey)
    }
}
