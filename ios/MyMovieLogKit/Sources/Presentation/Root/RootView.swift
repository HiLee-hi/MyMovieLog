import Domain
import SwiftUI

enum AppTab: Hashable {
    case home
    case search
    case library
    case profile
}

/// Tab bar of the app. On iPad the same TabView is shown as the iPadOS tab bar / sidebar.
public struct RootView: View {
    private let useCases: UseCaseContainer
    @State private var appViewModel: AppViewModel
    @State private var selectedTab: AppTab = .home
    @State private var libraryStatus: WatchStatus = .watched

    public init(useCases: UseCaseContainer) {
        PopcornTheme.install()
        self.useCases = useCases
        _appViewModel = State(initialValue: AppViewModel(
            observeCurrentUser: useCases.observeCurrentUser,
            syncFromRemote: useCases.syncFromRemote,
            clearLocalCache: useCases.clearLocalCache
        ))
    }

    public var body: some View {
        TabView(selection: $selectedTab) {
            HomeView(
                useCases: useCases,
                isLoggedIn: appViewModel.isLoggedIn,
                onShowLibrary: showLibrary,
                onNavigateToLogin: navigateToLogin,
                onRefresh: appViewModel.refresh
            )
            .tabItem { Label(L10n.tabHome, systemImage: "house") }
            .tag(AppTab.home)

            SearchView(
                useCases: useCases,
                isLoggedIn: appViewModel.isLoggedIn,
                onNavigateToLogin: navigateToLogin
            )
            .tabItem { Label(L10n.tabSearch, systemImage: "magnifyingglass") }
            .tag(AppTab.search)

            LibraryView(
                useCases: useCases,
                status: $libraryStatus,
                isLoggedIn: appViewModel.isLoggedIn,
                onNavigateToLogin: navigateToLogin,
                onRefresh: appViewModel.refresh
            )
            .tabItem { Label(L10n.tabLibrary, systemImage: "film.stack") }
            .tag(AppTab.library)

            ProfileView(useCases: useCases, onLoginSuccess: { selectedTab = .home })
                .tabItem { Label(L10n.tabProfile, systemImage: "person") }
                .tag(AppTab.profile)
        }
        // The design has a single light theme, the same as the Android app
        .tint(.popcornCaramel)
        .preferredColorScheme(.light)
        .task { await appViewModel.observeAuthAndSync() }
    }

    private func showLibrary(_ status: WatchStatus) {
        libraryStatus = status
        selectedTab = .library
    }

    private func navigateToLogin() {
        selectedTab = .profile
    }
}
