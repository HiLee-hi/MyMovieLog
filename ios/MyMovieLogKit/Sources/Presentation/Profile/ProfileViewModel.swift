import Domain
import Foundation
import Observation

public enum EmailAuthState: Equatable, Sendable {
    case idle
    case loading
    case success
    /// Sign-up finished; the user has to open the link in the verification e-mail first
    case emailVerificationRequired
    case error(String)
}

@MainActor
@Observable
public final class ProfileViewModel {
    public private(set) var currentUser: UserProfile?
    public private(set) var emailAuthState: EmailAuthState = .idle
    public private(set) var googleError: String?

    private let observeCurrentUser: ObserveCurrentUserUseCase
    private let signInWithEmailUseCase: SignInWithEmailUseCase
    private let signUpWithEmailUseCase: SignUpWithEmailUseCase
    private let signInWithGoogleUseCase: SignInWithGoogleUseCase
    private let signOutUseCase: SignOutUseCase

    public init(
        observeCurrentUser: ObserveCurrentUserUseCase,
        signInWithEmail: SignInWithEmailUseCase,
        signUpWithEmail: SignUpWithEmailUseCase,
        signInWithGoogle: SignInWithGoogleUseCase,
        signOut: SignOutUseCase
    ) {
        self.observeCurrentUser = observeCurrentUser
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

    /// True when the user came back signed in
    @discardableResult
    public func signInWithGoogle() async -> Bool {
        googleError = nil
        do {
            try await signInWithGoogleUseCase()
            return true
        } catch {
            // Closing the Google sheet is not an error worth showing
            if !Self.isUserCancellation(error) {
                googleError = error.localizedDescription
            }
            return false
        }
    }

    /// ASWebAuthenticationSessionError.canceledLogin, without importing AuthenticationServices here
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
