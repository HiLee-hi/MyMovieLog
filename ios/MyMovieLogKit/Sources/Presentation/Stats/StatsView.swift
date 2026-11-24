import Domain
import SwiftUI

// totals, average rating, this vs last month, last 6 months
// wide: summary | chart, like android StatsScreen
struct StatsView: View {
    private let isLoggedIn: Bool
    private let onNavigateToLogin: () -> Void
    private let onRefresh: () async -> Void
    @State private var viewModel: StatsViewModel
    @State private var screenWidth: CGFloat = 0

    init(
        useCases: UseCaseContainer,
        isLoggedIn: Bool,
        onNavigateToLogin: @escaping () -> Void,
        onRefresh: @escaping () async -> Void
    ) {
        self.isLoggedIn = isLoggedIn
        self.onNavigateToLogin = onNavigateToLogin
        self.onRefresh = onRefresh
        _viewModel = State(initialValue: StatsViewModel(getRecords: useCases.getRecords))
    }

    var body: some View {
        NavigationStack {
            Group {
                if isLoggedIn {
                    content
                } else {
                    LoginRequiredView(message: L10n.statsLoginRequired, onNavigateToLogin: onNavigateToLogin)
                }
            }
            .popcornScreenBackground()
            .navigationTitle(L10n.tabStats)
        }
        .task { await viewModel.observeRecords() }
    }

    private var isWide: Bool {
        AdaptiveDimens.fitsTwoColumns(min(screenWidth, AdaptiveDimens.contentMaxWidth) - 32)
    }

    private var content: some View {
        ScrollView {
            Group {
                if isWide {
                    AdaptiveTwoColumn {
                        summary
                    } second: {
                        monthlyChart
                    }
                } else {
                    VStack(alignment: .leading, spacing: 24) {
                        summary
                        monthlyChart
                    }
                }
            }
            .padding()
            .frame(maxWidth: isWide ? AdaptiveDimens.contentMaxWidth : 700)
            .frame(maxWidth: .infinity)
            .onWidthChange { screenWidth = $0 }
        }
        .refreshable { await onRefresh() }
    }

    // MARK: - Summary

    private var summary: some View {
        let state = viewModel.uiState
        return VStack(alignment: .leading, spacing: 24) {
            HStack(spacing: 10) {
                summaryCard(label: L10n.commonTotalWatched, highlighted: true) {
                    Text(L10n.commonMovieCount(state.totalWatched))
                }
                summaryCard(label: L10n.statsAverageRating, highlighted: false) {
                    HStack(spacing: 4) {
                        Image(systemName: "star.fill")
                            .font(.system(size: 17))
                            .foregroundStyle(Color.popcornStar)
                            .accessibilityHidden(true)
                        Text(state.averageRatingText)
                    }
                }
                summaryCard(label: L10n.statsWishlist, highlighted: false) {
                    Text(L10n.commonMovieCount(state.wishlistCount))
                }
            }

            VStack(alignment: .leading, spacing: 12) {
                sectionTitle(L10n.commonThisMonth)
                thisMonthCard(state)
            }
        }
    }

    // highlighted = butter fill, others = white outlined
    private func summaryCard<Value: View>(
        label: String,
        highlighted: Bool,
        @ViewBuilder value: () -> Value
    ) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.footnote.weight(.medium))
                .foregroundStyle(highlighted ? Color.popcornInk : Color.popcornInkMuted)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            value()
                .font(.jua(26))
                .foregroundStyle(Color.popcornInk)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
                .contentTransition(.numericText())
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(14)
        .popcornCard(fill: highlighted ? .popcornButter : .popcornSurface, outlined: !highlighted)
        .accessibilityElement(children: .combine)
    }

    private func thisMonthCard(_ state: StatsUiState) -> some View {
        HStack(spacing: 14) {
            PopcornMark(size: 44)
            VStack(alignment: .leading, spacing: 0) {
                Text(L10n.statsWatchedMovies)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Color.popcornInkMuted)
                Text(L10n.commonMovieCount(state.thisMonthCount))
                    .font(.jua(30))
                    .foregroundStyle(Color.popcornInk)
                    .contentTransition(.numericText())
            }
            Spacer(minLength: 8)
            comparisonText(state.comparison)
        }
        .padding(.horizontal, 18)
        .padding(.vertical, 16)
        .popcornCard(fill: .popcornButterLight, outlined: false)
        .accessibilityElement(children: .combine)
    }

    // "지난달보다 / 3편 더", amount in caramel
    @ViewBuilder
    private func comparisonText(_ comparison: MonthComparison) -> some View {
        switch comparison {
        case .none:
            EmptyView()
        case .same:
            Text(L10n.statsSameAsLastMonth)
                .font(.caption)
                .foregroundStyle(Color.popcornInkMuted)
                .multilineTextAlignment(.trailing)
        case .more(let amount), .fewer(let amount):
            let emphasis = if case .more = comparison {
                L10n.statsVsLastMonthMore(amount)
            } else {
                L10n.statsVsLastMonthFewer(amount)
            }
            VStack(alignment: .trailing, spacing: 1) {
                Text(L10n.statsVsLastMonth)
                    .foregroundStyle(Color.popcornInkMuted)
                Text(emphasis)
                    .fontWeight(.bold)
                    .foregroundStyle(Color.popcornCaramel)
            }
            .font(.caption)
        }
    }

    // MARK: - Chart

    private var monthlyChart: some View {
        let state = viewModel.uiState
        return VStack(alignment: .leading, spacing: 12) {
            sectionTitle(L10n.statsRecentSixMonths)
            VStack(spacing: 8) {
                HStack(alignment: .bottom, spacing: 10) {
                    ForEach(Array(state.monthlyCounts.enumerated()), id: \.offset) { index, item in
                        MonthBar(
                            item: item,
                            maximum: state.chartMaximum,
                            isCurrent: index == state.monthlyCounts.count - 1
                        )
                    }
                }
                .frame(height: MonthBar.maxBarHeight + 24, alignment: .bottom)

                Divider().overlay(Color.popcornOutline)

                HStack(spacing: 10) {
                    ForEach(Array(state.monthlyCounts.enumerated()), id: \.offset) { index, item in
                        let isCurrent = index == state.monthlyCounts.count - 1
                        Text(item.month.monthText)
                            .font(.caption.weight(isCurrent ? .bold : .regular))
                            .foregroundStyle(isCurrent ? Color.popcornInk : Color.popcornInkMuted)
                            .frame(maxWidth: .infinity)
                    }
                }
                .accessibilityHidden(true)
            }
            .padding(.horizontal, 16)
            .padding(.top, 20)
            .padding(.bottom, 14)
            .popcornCard()
        }
    }

    private func sectionTitle(_ text: String) -> some View {
        Text(text)
            .font(.headline)
            .foregroundStyle(Color.popcornInk)
            .accessibilityAddTraits(.isHeader)
    }
}

// bar height relative to the busiest month
private struct MonthBar: View {
    static let maxBarHeight: CGFloat = 120

    let item: MonthCount
    let maximum: Int
    let isCurrent: Bool

    var body: some View {
        VStack(spacing: 6) {
            Text(L10n.commonMovieCount(item.count))
                .font(.caption.weight(isCurrent ? .bold : .regular))
                .foregroundStyle(isCurrent ? Color.popcornInk : Color.popcornInkMuted)
                .lineLimit(1)
                .minimumScaleFactor(0.7)
            UnevenRoundedRectangle(topLeadingRadius: 10, bottomLeadingRadius: 4, bottomTrailingRadius: 4, topTrailingRadius: 10)
                .fill(isCurrent ? Color.popcornButter : Color.popcornButterSoft)
                .frame(maxWidth: 34)
                .frame(height: barHeight)
        }
        .frame(maxWidth: .infinity)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(L10n.statsMonthBar(item.month.monthText, item.count))
    }

    // thin sliver for 0 so the column still shows
    private var barHeight: CGFloat {
        item.count == 0 ? 3 : max(Self.maxBarHeight * CGFloat(item.count) / CGFloat(maximum), 8)
    }
}
