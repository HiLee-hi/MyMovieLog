import Domain
import SwiftUI

struct MovieRoute: Hashable {
    let movieId: Int
}

struct SearchView: View {
    private let useCases: UseCaseContainer
    private let isLoggedIn: Bool
    private let onNavigateToLogin: () -> Void
    @State private var viewModel: SearchViewModel
    @State private var selection = ListDetailSelection<MovieRoute>()
    @State private var screenWidth: CGFloat = 0

    init(useCases: UseCaseContainer, isLoggedIn: Bool, onNavigateToLogin: @escaping () -> Void) {
        self.useCases = useCases
        self.isLoggedIn = isLoggedIn
        self.onNavigateToLogin = onNavigateToLogin
        _viewModel = State(initialValue: SearchViewModel(searchMovies: useCases.searchMovies))
    }

    // wide: detail beside the results, not pushed
    private var isTwoPane: Bool { AdaptiveDimens.fitsListDetail(screenWidth) }

    var body: some View {
        NavigationStack(path: $selection.path) {
            HStack(spacing: 0) {
                VStack(spacing: 0) {
                    PopcornSearchField(text: $viewModel.query, prompt: L10n.searchPrompt)
                        .padding(.horizontal)
                        .padding(.top, 4)
                        .padding(.bottom, 8)
                        .frame(maxWidth: 700)
                    content
                }
                    .frame(width: isTwoPane ? AdaptiveDimens.listPaneWidth : nil)
                    .frame(maxWidth: isTwoPane ? nil : .infinity, maxHeight: .infinity)
                if isTwoPane {
                    Rectangle()
                        .fill(Color.popcornOutline)
                        .frame(width: 1)
                        .ignoresSafeArea(edges: .bottom)
                    detailPane
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                }
            }
            .onWidthChange { screenWidth = $0 }
            // fold keeps the open movie: side pane <-> pushed screen
            .onChange(of: isTwoPane) { _, twoPane in selection.adapt(isTwoPane: twoPane) }
            .popcornScreenBackground()
            .navigationTitle(L10n.tabSearch)
            .task(id: viewModel.query) {
                // 400 ms debounce, same as android SearchViewModel
                try? await Task.sleep(for: .milliseconds(400))
                guard !Task.isCancelled else { return }
                await viewModel.search()
            }
            .navigationDestination(for: MovieRoute.self) { route in
                MovieDetailView(
                    movieId: route.movieId,
                    useCases: useCases,
                    isLoggedIn: isLoggedIn,
                    onNavigateToLogin: onNavigateToLogin
                )
            }
        }
    }

    @ViewBuilder
    private var detailPane: some View {
        if let route = selection.selected {
            MovieDetailView(
                movieId: route.movieId,
                useCases: useCases,
                isLoggedIn: isLoggedIn,
                onNavigateToLogin: onNavigateToLogin,
                isEmbedded: true
            )
            // new movie -> new view model
            .id(route)
        } else {
            PopcornEmptyState(title: L10n.searchSelectTitle, systemImage: "film", message: L10n.searchSelectMessage)
        }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.phase {
        case .idle:
            PopcornEmptyState(title: L10n.searchStart, systemImage: "magnifyingglass")
        case .loading:
            ProgressView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
        case .empty:
            PopcornEmptyState(title: L10n.searchNoResults, systemImage: "film")
        case .error(let message):
            PopcornEmptyState(title: L10n.searchFailed, systemImage: "exclamationmark.triangle", message: message,
                              actionTitle: L10n.commonRetry) { Task { await viewModel.search() } }
        case .loaded:
            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(viewModel.movies) { movie in
                        let route = MovieRoute(movieId: movie.id)
                        Button {
                            selection.open(route, isTwoPane: isTwoPane)
                        } label: {
                            SearchResultRow(movie: movie, isSelected: isTwoPane && selection.selected == route)
                        }
                        .buttonStyle(.plain)
                        .accessibilityAddTraits(isTwoPane && selection.selected == route ? .isSelected : [])
                        .task { await viewModel.loadMoreIfNeeded(after: movie) }
                    }
                    if viewModel.isLoadingMore {
                        ProgressView()
                            .frame(maxWidth: .infinity)
                            .padding()
                    }
                }
                .padding(.horizontal)
                .padding(.vertical, 8)
                .frame(maxWidth: 700)
                .frame(maxWidth: .infinity)
            }
            .scrollDismissesKeyboard(.immediately)
        }
    }
}

private struct SearchResultRow: View {
    let movie: Movie
    var isSelected = false

    var body: some View {
        HStack(alignment: .top, spacing: 14) {
            PosterImage(url: movie.posterURL, cornerRadius: PopcornRadius.small)
                .frame(width: 64)
            VStack(alignment: .leading, spacing: 4) {
                Text(movie.title)
                    .font(.body.weight(.bold))
                    .foregroundStyle(Color.popcornInk)
                HStack(spacing: 6) {
                    if !movie.originalTitle.isEmpty && movie.originalTitle != movie.title {
                        Text(movie.originalTitle)
                            .lineLimit(1)
                            .foregroundStyle(Color.popcornInkMuted)
                    }
                    if let year = movie.releaseYear {
                        Text(year)
                            .fontWeight(.medium)
                            .foregroundStyle(Color.popcornCaramel)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 2)
                            .background(isSelected ? Color.popcornSurface : Color.popcornButterLight, in: Capsule())
                    }
                }
                .font(.caption)
                Text(movie.overview)
                    .font(.caption)
                    .foregroundStyle(Color.popcornInkMuted)
                    .lineLimit(2)
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .popcornCard(fill: isSelected ? .popcornButterLight : .popcornSurface)
        .overlay {
            if isSelected {
                RoundedRectangle(cornerRadius: PopcornRadius.large)
                    .strokeBorder(Color.popcornButter, lineWidth: 1.5)
            }
        }
        .contentShape(RoundedRectangle(cornerRadius: PopcornRadius.large))
    }
}
