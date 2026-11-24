import Domain
import Foundation
import Testing
@testable import Presentation

@Suite struct L10nTests {
    private static let sources = URL(filePath: #filePath)
        .deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
        .appending(path: "Sources")

    private var isEnglish: Bool { L10n.locale.language.languageCode == .english }

    private func placeholders(_ text: String) -> [String] {
        text.matches(of: /%(?:\d+\$)?(?:lld|@)/).map { String($0.output) }.sorted()
    }

    @Test(arguments: ["Presentation", "Domain", "DataLayer"])
    func catalogHasEveryKeyInBothLanguages(target: String) throws {
        let url = Self.sources.appending(path: "\(target)/Resources/Localizable.xcstrings")
        let catalog = try #require(JSONSerialization.jsonObject(with: Data(contentsOf: url)) as? [String: Any])
        let strings = try #require(catalog["strings"] as? [String: [String: Any]])
        #expect(!strings.isEmpty)

        for (key, entry) in strings {
            let localizations = try #require(entry["localizations"] as? [String: [String: [String: String]]], "\(key)")
            let korean = try #require(localizations["ko"]?["stringUnit"]?["value"], "\(key) has no Korean text")
            let english = try #require(localizations["en"]?["stringUnit"]?["value"], "\(key) has no English text")
            #expect(!english.isEmpty, "\(key)")
            #expect(placeholders(korean) == placeholders(english), "\(key)")
        }
    }

    @Test func resolvesTextForTheCurrentLanguage() {
        #expect(L10n.appName == (isEnglish ? "Popcorn Log" : "팝콘한알"))
        #expect(L10n.commonMovieCount(3) == (isEnglish ? "3" : "3편"))
        #expect(L10n.recordDeleteConfirm("Dune") == (isEnglish ? "Delete the record for \"Dune\"?" : "\"Dune\" 기록을 삭제할까요?"))
        #expect(L10n.weekdays.count == 7)
    }

    @Test func formatsDatesForTheCurrentLanguage() {
        let date = LocalDate(year: 2026, month: 10, day: 28)
        #expect(date.displayText == (isEnglish ? "Oct 28, 2026" : "2026년 10월 28일"))
        #expect(date.yearMonth.displayText == (isEnglish ? "October 2026" : "2026년 10월"))
    }
}
