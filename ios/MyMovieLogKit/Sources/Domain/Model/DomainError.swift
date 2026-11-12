import Foundation

public enum DomainError: LocalizedError, Equatable {
    case notSignedIn

    public var errorDescription: String? {
        switch self {
        case .notSignedIn: return "로그인이 필요해요"
        }
    }
}
