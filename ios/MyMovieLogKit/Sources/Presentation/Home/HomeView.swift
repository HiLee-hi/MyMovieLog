import Domain
import SwiftUI

private enum HomeRoute: Hashable {
    case calendar
}

struct HomeView: View {
    private let useCases: UseCaseContainer
    private let isLoggedIn: Bool
    private let onShowLibrary: (WatchStatus) -> Void
    private let onNavigateToLogin: () -> Void
    private let onRefresh: () async -> Void
    @State private var viewModel: HomeViewModel
    @State private var screenWidth: CGFloat = 0

    init(
        useCases: UseCaseContainer,
        isLoggedIn: Bool,
        onShowLibrary: @escaping (WatchStatus) -> Void,
        onNavigateToLogin: @escaping () -> Void,
        onRefresh: @escaping () async -> Void
    ) {
        self.useCases = useCases
        self.isLoggedIn = isLoggedIn
        self.onShowLibrary = onShowLibrary
        self.onNavigateToLogin = onNavigateToLogin
        self.onRefresh = onRefresh
        _viewModel = State(initialValue: HomeViewModel(
            getRecords: useCases.getRecords,
            upsertRecord: useCases.upsertRecord
        ))
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    VStack(alignment: .leading, spacing: 8) {
                        header
                        Text("오늘은 어떤 영화를 봤나요?")
                            .font(.subheadline)
                            .foregroundStyle(Color.popcornInkMuted)
                    }
                    if !isLoggedIn {
                        loginBanner
                    }
                    if isWide {
                        // Unfolded or wide windows: records on the left, the summary beside them
                        AdaptiveTwoColumn(firstFraction: 0.62) {
                            VStack(alignment: .leading, spacing: 24) { recordSections }
                        } second: {
                            summaryPanel
                        }
                    } else {
                        statCards
                        recordSections
                    }
                }
                .padding()
                .frame(maxWidth: isWide ? AdaptiveDimens.contentMaxWidth : 700)
                .frame(maxWidth: .infinity)
                .onWidthChange { screenWidth = $0 }
            }
            .popcornScreenBackground()
            .refreshable { await onRefresh() }
            .hidesNavigationBar(true)
            .navigationTitle("팝콘한알")
            .navigationDestination(for: HomeRoute.self) { _ in
                CalendarView(useCases: useCases, isLoggedIn: isLoggedIn, onNavigateToLogin: onNavigateToLogin)
                    .hidesNavigationBar(false)
            }
            .sheet(isPresented: viewModel.editSession.isPresentedBinding) {
                viewModel.editSession.editorSheet()
            }
            .task { await viewModel.observeRecords() }
        }
    }

    /// Brand mark, app name and the calendar shortcut, in place of the navigation bar
    private var header: some View {
        HStack(spacing: 10) {
            PopcornMark()
            Text("팝콘한알")
                .font(.jua(26, relativeTo: .largeTitle))
                .foregroundStyle(Color.popcornInk)
                .accessibilityAddTraits(.isHeader)
            Spacer()
            NavigationLink(value: HomeRoute.calendar) {
                Image(systemName: "calendar")
                    .font(.title3)
                    .foregroundStyle(Color.popcornInk)
                    .frame(width: 44, height: 44)
                    .background(Color.popcornSurface, in: Circle())
                    .overlay { Circle().strokeBorder(Color.popcornOutline) }
            }
            .buttonStyle(.plain)
            .accessibilityLabel("관람 캘린더")
        }
    }

    private var loginBanner: some View {
        HStack(spacing: 12) {
            Image(systemName: "person.crop.circle.badge.plus")
                .font(.title2)
                .foregroundStyle(Color.popcornCaramel)
            Text("로그인하고 감상 기록을 시작해보세요")
                .font(.subheadline)
            Spacer()
            Button("로그인", action: onNavigateToLogin)
                .buttonStyle(.bordered)
        }
        .padding()
        .popcornCard(fill: .popcornButterLight, outlined: false)
    }

    private var isWide: Bool {
        AdaptiveDimens.fitsTwoColumns(min(screenWidth, AdaptiveDimens.contentMaxWidth) - 32)
    }

    @ViewBuilder
    private var recordSections: some View {
        recordSection(
            title: "최근에 본 영화",
            records: viewModel.uiState.recentWatched,
            status: .watched,
            emptyMessage: "아직 본 영화 기록이 없어요"
        )
        recordSection(
            title: "보고 싶은 영화",
            records: viewModel.uiState.wishlistPreview,
            status: .wishlist,
            emptyMessage: "보고 싶은 영화를 추가해보세요"
        )
    }

    /// Side column on wide windows: the stat cards stacked, with the calendar shortcut below
    private var summaryPanel: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("나의 감상 요약")
                .font(.headline)
                .foregroundStyle(Color.popcornInk)
            statCard(value: viewModel.uiState.thisMonthCount, label: "이번 달", highlighted: true)
            statCard(value: viewModel.uiState.totalWatched, label: "총 감상", highlighted: false)
            NavigationLink(value: HomeRoute.calendar) {
                Label("관람 캘린더 보기", systemImage: "calendar")
                    .font(.subheadline.weight(.bold))
                    .foregroundStyle(Color.popcornInk)
                    .frame(maxWidth: .infinity, minHeight: 48)
                    .background(Color.popcornSurface, in: Capsule())
                    .overlay { Capsule().strokeBorder(Color.popcornOutlineStrong) }
            }
            .buttonStyle(.plain)
        }
    }

    private var statCards: some View {
        HStack(spacing: 12) {
            statCard(value: viewModel.uiState.thisMonthCount, label: "이번 달", highlighted: true)
            statCard(value: viewModel.uiState.totalWatched, label: "총 감상", highlighted: false)
        }
    }

    /// The highlighted card is filled with butter; the other one is a white outlined card
    private func statCard(value: Int, label: String, highlighted: Bool) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(highlighted ? Color.popcornInk : Color.popcornInkMuted)
            Text("\(value)편")
                .font(.jua(30))
                .foregroundStyle(Color.popcornInk)
                .contentTransition(.numericText())
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .popcornCard(fill: highlighted ? .popcornButter : .popcornSurface, outlined: !highlighted)
        .accessibilityElement(children: .combine)
    }

    private func recordSection(
        title: String,
        records: [MovieRecord],
        status: WatchStatus,
        emptyMessage: String
    ) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(title)
                    .font(.headline)
                    .foregroundStyle(Color.popcornInk)
                Spacer()
                if !records.isEmpty {
                    Button("전체 보기") { onShowLibrary(status) }
                        .buttonStyle(.plain)
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Color.popcornCaramel)
                }
            }
            if records.isEmpty {
                Text(emptyMessage)
                    .font(.subheadline)
                    .foregroundStyle(Color.popcornInkMuted)
                    .frame(maxWidth: .infinity, minHeight: 80)
                    .popcornCard(fill: .popcornButterLight, outlined: false)
            } else {
                ScrollView(.horizontal, showsIndicators: false) {
                    LazyHStack(alignment: .top, spacing: 12) {
                        ForEach(records) { record in
                            Button {
                                viewModel.editSession.select(record)
                            } label: {
                                RecordPosterCell(record: record)
                                    .frame(width: 112)
                            }
                            .buttonStyle(.plain)
                        }
                    }
                    // Room for the poster shadow
                    .padding(.bottom, 8)
                }
            }
        }
    }
}

/// Poster, title and rating; used in the Home rows and the Library grid
struct RecordPosterCell: View {
    let record: MovieRecord

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            PosterImage(url: record.posterURL)
                .shadow(color: .popcornShadow, radius: 8, y: 4)
            Text(record.title)
                .font(.subheadline.weight(.medium))
                .foregroundStyle(Color.popcornInk)
                .lineLimit(1)
            RatingLabel(rating: record.rating)
        }
        .accessibilityElement(children: .combine)
    }
}
