import Domain
import SwiftUI

struct MovieDetailView: View {
    private let isLoggedIn: Bool
    private let onNavigateToLogin: () -> Void
    /// Shown inside the search screen's side pane, so it leaves that screen's navigation bar alone
    private let isEmbedded: Bool
    @State private var viewModel: MovieDetailViewModel
    @State private var width: CGFloat = 0

    init(
        movieId: Int,
        useCases: UseCaseContainer,
        isLoggedIn: Bool,
        onNavigateToLogin: @escaping () -> Void,
        isEmbedded: Bool = false
    ) {
        self.isLoggedIn = isLoggedIn
        self.onNavigateToLogin = onNavigateToLogin
        self.isEmbedded = isEmbedded
        _viewModel = State(initialValue: MovieDetailViewModel(
            movieId: movieId,
            getMovieDetail: useCases.getMovieDetail,
            getRecordByTmdbId: useCases.getRecordByTmdbId,
            upsertRecord: useCases.upsertRecord,
            getCurrentUserId: useCases.getCurrentUserId
        ))
    }

    var body: some View {
        Group {
            switch viewModel.phase {
            case .loading:
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            case .error(let message):
                PopcornEmptyState(title: L10n.detailLoadFailed, systemImage: "exclamationmark.triangle", message: message,
                                  actionTitle: L10n.commonRetry) { Task { await viewModel.loadDetail() } }
            case .loaded(let movie):
                if AdaptiveDimens.fitsTwoColumns(width - 64) {
                    wideContent(movie)
                } else {
                    content(movie)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .onWidthChange { width = $0 }
        .modifier(DetailNavigationBar(isEnabled: !isEmbedded))
        .background(Color.popcornCream)
        .task { await viewModel.loadDetail() }
        .task { await viewModel.observeRecord() }
        .sheet(isPresented: $viewModel.isEditorPresented, onDismiss: viewModel.dismissEditor) {
            RecordEditorSheet(
                title: viewModel.movie?.title ?? "",
                draft: $viewModel.draft,
                saveState: viewModel.saveState,
                photoPaths: viewModel.existingRecord?.photoUrls ?? [],
                onSave: viewModel.saveRecord,
                onCancel: viewModel.dismissEditor
            )
        }
    }

    private func content(_ movie: Movie) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                header(movie, backdropHeight: isEmbedded ? 180 : 240)
                VStack(alignment: .leading, spacing: 12) {
                    GenreChips(genres: movie.genres)
                    if !movie.overview.isEmpty {
                        overviewCard(movie.overview)
                    }
                    if let record = viewModel.existingRecord {
                        recordCard(record)
                    }
                }
                .padding(.horizontal, 20)
            }
            .padding(.bottom, 24)
            .frame(maxWidth: 760)
            .frame(maxWidth: .infinity)
        }
        .ignoresSafeArea(edges: isEmbedded ? [] : .top)
        .safeAreaInset(edge: .bottom) {
            recordButton
                .padding(.horizontal, 20)
                .padding(.vertical, 12)
                .background(Color.popcornCream)
        }
    }

    /// Unfolded or wide windows: movie information on the left, my log with its button on the right
    private func wideContent(_ movie: Movie) -> some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                backdrop(movie, height: 170)
                AdaptiveTwoColumn(firstFraction: 0.55) {
                    VStack(alignment: .leading, spacing: 14) {
                        HStack(alignment: .bottom, spacing: 16) {
                            poster(movie, width: 120)
                            titleInfo(movie)
                                .padding(.bottom, 4)
                        }
                        GenreChips(genres: movie.genres)
                        if !movie.overview.isEmpty {
                            overviewCard(movie.overview)
                        }
                    }
                } second: {
                    myLogPane
                        // Starts below the backdrop, level with the title
                        .padding(.top, wideOverlap + 12)
                }
                .padding(.horizontal, 32)
                .padding(.top, -wideOverlap)
            }
            .padding(.bottom, 24)
            .frame(maxWidth: AdaptiveDimens.contentMaxWidth)
            .frame(maxWidth: .infinity)
        }
        .ignoresSafeArea(edges: isEmbedded ? [] : .top)
    }

    private var myLogPane: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(L10n.detailMyRecord)
                .font(.headline)
                .foregroundStyle(Color.popcornInk)
            if let record = viewModel.existingRecord {
                RecordSummary(record: record)
            } else {
                Text(L10n.detailNoRecord)
                    .font(.subheadline)
                    .foregroundStyle(Color.popcornInkMuted)
            }
            recordButton
                .padding(.top, 6)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(18)
        .popcornCard(fill: .popcornButterLight, outlined: false)
    }

    /// Backdrop with the poster overlapping its bottom edge; the title starts below the image
    private func header(_ movie: Movie, backdropHeight: CGFloat) -> some View {
        ZStack(alignment: .topLeading) {
            backdrop(movie, height: backdropHeight)
            HStack(alignment: .top, spacing: 16) {
                poster(movie, width: 112)
                titleInfo(movie)
                    .padding(.top, posterOverlap + 8)
            }
            .padding(.horizontal, 20)
            .padding(.top, backdropHeight - posterOverlap)
        }
    }

    private func poster(_ movie: Movie, width: CGFloat) -> some View {
        PosterImage(url: movie.posterURL)
            .frame(width: width)
            .overlay {
                RoundedRectangle(cornerRadius: PopcornRadius.medium)
                    .strokeBorder(Color.popcornCream, lineWidth: 3)
            }
            .shadow(color: .popcornShadow, radius: 10, y: 6)
    }

    private func titleInfo(_ movie: Movie) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(movie.title)
                .font(.jua(24))
                .foregroundStyle(Color.popcornInk)
                .accessibilityAddTraits(.isHeader)
            let meta = [
                movie.originalTitle.isEmpty || movie.originalTitle == movie.title ? nil : movie.originalTitle,
                movie.releaseYear
            ].compactMap { $0 }.joined(separator: " · ")
            if !meta.isEmpty {
                Text(meta)
                    .font(.subheadline)
                    .foregroundStyle(Color.popcornInkMuted)
            }
            if movie.voteAverage > 0 {
                HStack(spacing: 4) {
                    Image(systemName: "star.fill")
                        .foregroundStyle(Color.popcornStar)
                    Text(String(format: "%.1f", movie.voteAverage))
                        .fontWeight(.medium)
                        .foregroundStyle(Color.popcornInk)
                    Text(verbatim: "TMDB")
                        .foregroundStyle(Color.popcornInkMuted)
                }
                .font(.subheadline)
                .accessibilityElement(children: .combine)
            }
        }
    }

    private func overviewCard(_ overview: String) -> some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(L10n.detailOverview)
                .font(.headline)
                .foregroundStyle(Color.popcornInk)
            Text(overview)
                .font(.subheadline)
                .foregroundStyle(Color.popcornInkMuted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .popcornCard()
    }

    private func recordCard(_ record: MovieRecord) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(L10n.detailMyRecord)
                .font(.headline)
                .foregroundStyle(Color.popcornInk)
            RecordSummary(record: record)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(16)
        .popcornCard(fill: .popcornButterLight, outlined: false)
    }

    private func backdrop(_ movie: Movie, height: CGFloat) -> some View {
        Color.popcornButter
            .frame(height: height)
            .overlay {
                if let backdropURL = movie.backdropURL ?? movie.posterURL {
                    AsyncImage(url: backdropURL) { image in
                        image.resizable().scaledToFill()
                    } placeholder: {
                        Color.clear
                    }
                }
            }
            .overlay {
                // Keeps the back button readable on top and fades into the background below
                VStack(spacing: 0) {
                    if !isEmbedded {
                        LinearGradient(colors: [Color.popcornCream.opacity(0.8), .clear], startPoint: .top, endPoint: .bottom)
                            .frame(height: 110)
                    }
                    Spacer()
                    LinearGradient(colors: [.clear, Color.popcornCream], startPoint: .top, endPoint: .bottom)
                        .frame(height: 72)
                }
            }
            .clipped()
            .accessibilityHidden(true)
    }

    private var posterOverlap: CGFloat { 72 }
    /// How far the wide layout's poster row reaches up into the backdrop
    private var wideOverlap: CGFloat { 96 }

    private var recordButton: some View {
        Button {
            if isLoggedIn {
                viewModel.openEditor()
            } else {
                onNavigateToLogin()
            }
        } label: {
            Text(viewModel.existingRecord == nil ? L10n.recordAdd : L10n.recordEdit)
        }
        .buttonStyle(.popcornPrimary)
        .disabled(viewModel.movie == nil)
    }
}

/// The backdrop shows through the bar, so the bar carries only the back button
private struct DetailNavigationBar: ViewModifier {
    let isEnabled: Bool

    func body(content: Content) -> some View {
        if isEnabled {
            content
                .navigationTitle(Text(verbatim: ""))
                .inlineNavigationTitle()
                .transparentNavigationBar()
        } else {
            content
        }
    }
}

private struct GenreChips: View {
    let genres: [String]

    var body: some View {
        if !genres.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(genres, id: \.self) { genre in
                        Text(genre)
                            .font(.caption.weight(.medium))
                            .foregroundStyle(Color.popcornCaramel)
                            .padding(.horizontal, 12)
                            .padding(.vertical, 6)
                            .background(Color.popcornButterLight, in: Capsule())
                    }
                }
            }
        }
    }
}

/// Read-only record details: status, rating, date, review or memo, photo count
struct RecordSummary: View {
    let record: MovieRecord

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(record.status == .watched ? L10n.recordStatusWatched : L10n.recordStatusWishlist)
                .font(.caption.weight(.semibold))
                .foregroundStyle(Color.popcornCaramel)
            if let rating = record.rating {
                RatingStars(rating: rating)
            }
            if let watchedAt = record.watchedAt {
                Text(L10n.detailWatchedOn(watchedAt.displayText))
                    .font(.subheadline)
                    .foregroundStyle(Color.popcornInkMuted)
            }
            if let text = record.review ?? record.memo {
                Text(text)
                    .font(.body)
                    .foregroundStyle(Color.popcornInk)
            }
            if !record.photoUrls.isEmpty {
                Label(L10n.detailPhotoCount(record.photoUrls.count), systemImage: "photo")
                    .font(.caption)
                    .foregroundStyle(Color.popcornInkMuted)
            }
        }
    }
}
