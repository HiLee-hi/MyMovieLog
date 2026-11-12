import Domain
import Foundation

extension LocalDate {
    /// "2026년 10월 28일"
    var koreanText: String {
        "\(year)년 \(month)월 \(day)일"
    }
}

extension YearMonth {
    /// "2026년 10월"
    var koreanText: String {
        "\(year)년 \(month)월"
    }
}
