@testable import DataLayer
import Foundation
import Testing

struct TmdbLanguageTests {
    @Test(arguments: [
        ("ko-KR", "ko"), ("en-US", "en"), ("en-KR", "en"), ("ja-JP", "ja"),
        ("pt-BR", "pt-BR"), ("zh-Hant-TW", "zh-TW"), ("zh-Hans-KR", "zh")
    ])
    func mapsDeviceLanguage(identifier: String, expected: String) {
        #expect(TmdbLanguage.tag(for: Locale(identifier: identifier)) == expected)
    }
}
