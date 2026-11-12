import SwiftUI

/// Shared measurements for wide windows (the iPhone Duo inner display, iPad, Mac),
/// the same numbers as the Android AdaptiveDimens so both apps switch layouts at the same widths.
enum AdaptiveDimens {
    /// Space between panes and columns
    static let paneSpacing: CGFloat = 24
    /// Preferred width of a list pane (search results, calendar)
    static let listPaneWidth: CGFloat = 360
    /// Smallest width a detail pane may shrink to before falling back to one pane
    static let detailPaneMinWidth: CGFloat = 360
    /// A column inside a screen (e.g. overview | my log) is never narrower than this
    static let columnMinWidth: CGFloat = 300
    /// Whole-screen content is capped so cards do not stretch across a wide display
    static let contentMaxWidth: CGFloat = 1040

    /// True when `width` fits two columns plus the spacer
    static func fitsTwoColumns(_ width: CGFloat, minColumnWidth: CGFloat = columnMinWidth) -> Bool {
        width >= minColumnWidth * 2 + paneSpacing
    }

    /// True when `width` fits a list pane next to a detail pane
    static func fitsListDetail(_ width: CGFloat) -> Bool {
        width >= listPaneWidth + paneSpacing + detailPaneMinWidth
    }
}

extension View {
    /// Reports the view's width whenever it changes (unfolding, rotation, Split View, window resize)
    func onWidthChange(_ action: @escaping (CGFloat) -> Void) -> some View {
        onGeometryChange(for: CGFloat.self) { proxy in
            proxy.size.width
        } action: { width in
            action(width)
        }
    }
}

/// Two columns side by side, the first taking `firstFraction` of the width
struct AdaptiveTwoColumn<First: View, Second: View>: View {
    var firstFraction: CGFloat = 0.5
    @ViewBuilder var first: First
    @ViewBuilder var second: Second

    var body: some View {
        // Layout splits the width by fraction without a GeometryReader taking all the height
        FractionalHStack(firstFraction: firstFraction, spacing: AdaptiveDimens.paneSpacing) {
            first
            second
        }
    }
}

/// Places exactly two subviews in a row; the first gets `firstFraction` of the width left after the spacing
private struct FractionalHStack: Layout {
    let firstFraction: CGFloat
    let spacing: CGFloat

    private func widths(for total: CGFloat) -> (CGFloat, CGFloat) {
        let available = max(total - spacing, 0)
        let first = (available * firstFraction).rounded(.down)
        return (first, available - first)
    }

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let total = proposal.width ?? AdaptiveDimens.columnMinWidth * 2 + spacing
        let (firstWidth, secondWidth) = widths(for: total)
        let heights = zip(subviews, [firstWidth, secondWidth]).map { subview, width in
            subview.sizeThatFits(ProposedViewSize(width: width, height: proposal.height)).height
        }
        return CGSize(width: total, height: heights.max() ?? 0)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        let (firstWidth, secondWidth) = widths(for: bounds.width)
        var x = bounds.minX
        for (subview, width) in zip(subviews, [firstWidth, secondWidth]) {
            subview.place(
                at: CGPoint(x: x, y: bounds.minY),
                anchor: .topLeading,
                proposal: ProposedViewSize(width: width, height: bounds.height)
            )
            x += width + spacing
        }
    }
}

/// Selected item of a list/detail screen that keeps its place when the window
/// switches between one pane (pushed detail) and two panes (detail beside the list).
struct ListDetailSelection<ID: Hashable>: Equatable {
    /// Detail shown beside the list on two panes
    var selected: ID?
    /// Navigation stack used on one pane
    var path: [ID] = []

    /// Moves the open detail between the side pane and the navigation stack
    mutating func adapt(isTwoPane: Bool) {
        if isTwoPane {
            if let last = path.last {
                selected = last
                path = []
            }
        } else if let selected {
            path = [selected]
            self.selected = nil
        }
    }

    mutating func open(_ id: ID, isTwoPane: Bool) {
        if isTwoPane {
            selected = id
        } else {
            path.append(id)
        }
    }
}
