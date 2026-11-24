import Auth
import Domain
import Foundation

final class AuthRepositoryImpl: AuthRepository {
    private let auth: AuthClient

    init(auth: AuthClient) {
        self.auth = auth
    }

    func observeCurrentUser() -> AsyncStream<UserProfile?> {
        let changes = auth.authStateChanges
        return AsyncStream { continuation in
            let task = Task {
                for await (event, session) in changes {
                    Log.auth.debug("Auth event: \(event.rawValue, privacy: .public)")
                    continuation.yield(session.map { Self.profile(from: $0.user) })
                }
                continuation.finish()
            }
            continuation.onTermination = { _ in task.cancel() }
        }
    }

    func currentUserId() async -> String? {
        // `session` refreshes an expired token; fall back to the stored user when offline
        let user = (try? await auth.session.user) ?? auth.currentUser
        return user?.id.uuidString.lowercased()
    }

    func signInWithEmail(email: String, password: String) async throws {
        Log.auth.info("Email sign-in attempt")
        do {
            try await auth.signIn(email: email, password: password)
            Log.auth.info("Email sign-in success")
        } catch {
            Log.auth.error("Email sign-in failed: \(error.localizedDescription, privacy: .public)")
            throw error
        }
    }

    func signUpWithEmail(email: String, password: String) async throws {
        Log.auth.info("Email sign-up attempt")
        do {
            try await auth.signUp(email: email, password: password)
            Log.auth.info("Email sign-up success")
        } catch {
            Log.auth.error("Email sign-up failed: \(error.localizedDescription, privacy: .public)")
            throw error
        }
    }

    func signInWithGoogle() async throws {
        Log.auth.info("Google sign-in attempt")
        do {
            try await auth.signInWithOAuth(provider: .google)
            Log.auth.info("Google sign-in success")
        } catch {
            Log.auth.error("Google sign-in failed: \(error.localizedDescription, privacy: .public)")
            throw error
        }
    }

    func signOut() async throws {
        Log.auth.info("Sign-out attempt")
        do {
            try await auth.signOut()
        } catch {
            Log.auth.error("Sign-out failed: \(error.localizedDescription, privacy: .public)")
            throw error
        }
    }

    private static func profile(from user: User) -> UserProfile {
        UserProfile(
            id: user.id.uuidString.lowercased(),
            email: user.email ?? "",
            displayName: user.userMetadata["full_name"]?.stringValue,
            avatarUrl: user.userMetadata["avatar_url"]?.stringValue
        )
    }
}
