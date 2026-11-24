import Foundation

// from Info.plist <- Config/Secrets.xcconfig <- android local.properties
public struct AppConfig: Sendable {
    public let tmdbApiKey: String
    public let supabaseURL: URL
    public let supabaseAnonKey: String
    // optional, without it the calendar shows weekends only
    public let publicDataApiKey: String?

    public init(tmdbApiKey: String, supabaseURL: URL, supabaseAnonKey: String, publicDataApiKey: String? = nil) {
        self.tmdbApiKey = tmdbApiKey
        self.supabaseURL = supabaseURL
        self.supabaseAnonKey = supabaseAnonKey
        self.publicDataApiKey = publicDataApiKey
    }

    // nil when missing or still the `$(...)` placeholder
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
        return AppConfig(
            tmdbApiKey: tmdbApiKey,
            supabaseURL: supabaseURL,
            supabaseAnonKey: supabaseAnonKey,
            publicDataApiKey: value("PUBLIC_DATA_API_KEY")
        )
    }
}
