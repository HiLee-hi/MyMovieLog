import Foundation

public enum DomainError: LocalizedError, Equatable {
    case notSignedIn

    public var errorDescription: String? {
        switch self {
        case .notSignedIn: return Bundle.module.localizedString(forKey: "error.not_signed_in", value: nil, table: nil)
        }
    }
}
