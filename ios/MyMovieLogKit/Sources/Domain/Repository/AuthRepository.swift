/// Streams emit the current value immediately and again after every change.
public protocol AuthRepository: Sendable {
    func observeCurrentUser() -> AsyncStream<UserProfile?>
    func currentUserId() async -> String?
    func signInWithEmail(email: String, password: String) async throws
    func signUpWithEmail(email: String, password: String) async throws
    func signInWithGoogle() async throws
    func signOut() async throws
}
