import Foundation

// TMDB `language` for the device language
enum TmdbLanguage {
    // region only where TMDB translations differ, e.g. en-KR has none of its own
    private static let regionalLanguages: [String: Set<String>] = [
        "pt": ["BR", "PT"],
        "zh": ["CN", "TW", "HK"]
    ]

    static var current: String {
        tag(for: Locale(identifier: Locale.preferredLanguages.first ?? "ko"))
    }

    static func tag(for locale: Locale) -> String {
        guard let language = locale.language.languageCode?.identifier(.alpha2) else { return "ko" }
        if let region = locale.language.region?.identifier, regionalLanguages[language]?.contains(region) == true {
            return "\(language)-\(region)"
        }
        return language
    }
}
