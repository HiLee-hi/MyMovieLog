import Domain
import Observation
import OSLog

// signed-in state + cache in step (sync after sign-in, clear after sign-out), like android AppViewModel
@MainActor
@Observable
public final class AppViewModel {
    public private(set) var isLoggedIn = false
    // false until the stored session is read, avoids a signed-out flash
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

    // pull-to-refresh + first sync after sign-in
    public func refresh() async {
        guard isLoggedIn else { return }
        do {
            try await syncFromRemote()
        } catch {
            logger.error("syncFromRemote failed: \(error.localizedDescription, privacy: .public)")
        }
    }
}
