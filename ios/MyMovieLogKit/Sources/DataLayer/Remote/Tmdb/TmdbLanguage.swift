import Foundation

/// TMDB `language` value for the device language.
enum TmdbLanguage {
    /// Languages whose TMDB translations differ by region. Others send no region, because a
    /// combination such as en-KR has no translation of its own.
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
