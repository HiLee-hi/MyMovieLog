import Domain
import Foundation
import Observation

public enum EmailAuthState: Equatable, Sendable {
    case idle
    case loading
    case success
    // signed up, waiting for the verification e-mail link
    case emailVerificationRequired
    case error(String)
}

// profile card numbers
public struct ProfileRecordCounts: Equatable, Sendable {
    public var watched = 0
    public var wishlist = 0

    public init(watched: Int = 0, wishlist: Int = 0) {
        self.watched = watched
        self.wishlist = wishlist
    }
}

@MainActor
@Observable
public final class ProfileViewModel {
    public private(set) var currentUser: UserProfile?
    public private(set) var recordCounts = ProfileRecordCounts()
    public private(set) var emailAuthState: EmailAuthState = .idle
    public private(set) var googleError: String?

    private let observeCurrentUser: ObserveCurrentUserUseCase
    private let getRecords: GetRecordsUseCase
    private let signInWithEmailUseCase: SignInWithEmailUseCase
    private let signUpWithEmailUseCase: SignUpWithEmailUseCase
    private let signInWithGoogleUseCase: SignInWithGoogleUseCase
    private let signOutUseCase: SignOutUseCase

    public init(
        observeCurrentUser: ObserveCurrentUserUseCase,
        getRecords: GetRecordsUseCase,
        signInWithEmail: SignInWithEmailUseCase,
        signUpWithEmail: SignUpWithEmailUseCase,
        signInWithGoogle: SignInWithGoogleUseCase,
        signOut: SignOutUseCase
    ) {
        self.observeCurrentUser = observeCurrentUser
        self.getRecords = getRecords
        signInWithEmailUseCase = signInWithEmail
        signUpWithEmailUseCase = signUpWithEmail
        signInWithGoogleUseCase = signInWithGoogle
        signOutUseCase = signOut
    }

    public func observeUser() async {
        for await user in observeCurrentUser() {
            currentUser = user
        }
    }

    public func observeRecordCounts() async {
        for await records in getRecords() {
            recordCounts = ProfileRecordCounts(
                watched: records.filter { $0.status == .watched }.count,
                wishlist: records.filter { $0.status == .wishlist }.count
            )
        }
    }

    public func signInWithEmail(email: String, password: String) async {
        emailAuthState = .loading
        do {
            try await signInWithEmailUseCase(email: email, password: password)
            emailAuthState = .success
        } catch {
            emailAuthState = .error(error.localizedDescription)
        }
    }

    public func signUpWithEmail(email: String, password: String) async {
        emailAuthState = .loading
        do {
            try await signUpWithEmailUseCase(email: email, password: password)
            emailAuthState = .emailVerificationRequired
        } catch {
            emailAuthState = .error(error.localizedDescription)
        }
    }

    // true if back signed in
    @discardableResult
    public func signInWithGoogle() async -> Bool {
        googleError = nil
        do {
            try await signInWithGoogleUseCase()
            return true
        } catch {
            // closing the Google sheet isn't an error
            if !Self.isUserCancellation(error) {
                googleError = error.localizedDescription
            }
            return false
        }
    }

    // ASWebAuthenticationSessionError.canceledLogin without importing AuthenticationServices
    private static func isUserCancellation(_ error: Error) -> Bool {
        let nsError = error as NSError
        return error is CancellationError
            || (nsError.domain == "com.apple.AuthenticationServices.WebAuthenticationSession" && nsError.code == 1)
    }

    public func signOut() async {
        try? await signOutUseCase()
        emailAuthState = .idle
    }

    public func resetEmailAuthState() {
        emailAuthState = .idle
    }
}
