import Domain
import Observation
import OSLog

/// Session-wide state: whether a user is signed in, and keeping the local cache in step with it
/// (sync after sign-in, clear after sign-out), as the Android AppViewModel does.
@MainActor
@Observable
public final class AppViewModel {
    public private(set) var isLoggedIn = false
    /// False until the stored session has been read, so the UI does not flash the signed-out state
    public private(set) var isSessionResolved = false

    private let observeCurrentUser: ObserveCurrentUserUseCase
    private let syncFromRemote: SyncFromRemoteUseCase
    private let clearLocalCache: ClearLocalCacheUseCase
    private let logger = Logger(subsystem: "com.mymovie.log", category: "APP_VM")

    public init(
        observeCurrentUser: ObserveCurrentUserUseCase,
        syncFromRemote: SyncFromRemoteUseCase,
        clearLocalCache: ClearLocalCacheUseCase
    ) {
        self.observeCurrentUser = observeCurrentUser
        self.syncFromRemote = syncFromRemote
        self.clearLocalCache = clearLocalCache
    }

    public func observeAuthAndSync() async {
        for await user in observeCurrentUser() {
            let loggedIn = user != nil
            let changed = !isSessionResolved || loggedIn != isLoggedIn
            isLoggedIn = loggedIn
            isSessionResolved = true
            guard changed else { continue }
            if loggedIn {
                logger.info("Login detected → starting syncFromRemote")
                await refresh()
            } else {
                logger.info("Logout detected → clearing local cache")
                try? await clearLocalCache()
            }
        }
    }

    /// Pull-to-refresh and the initial sync after sign-in
    public func refresh() async {
        guard isLoggedIn else { return }
        do {
            try await syncFromRemote()
        } catch {
            logger.error("syncFromRemote failed: \(error.localizedDescription, privacy: .public)")
        }
    }
}
