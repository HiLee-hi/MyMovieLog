import SwiftUI

// wide window thresholds (iPhone Duo inner, iPad, Mac), same numbers as android AdaptiveDimens
enum AdaptiveDimens {
    // pane / column spacing
    static let paneSpacing: CGFloat = 24
    // list pane width (search results, calendar)
    static let listPaneWidth: CGFloat = 360
    // below this -> one pane
    static let detailPaneMinWidth: CGFloat = 360
    // min width of a column inside a screen (e.g. overview | my log)
    static let columnMinWidth: CGFloat = 300
    // content width cap for wide displays
    static let contentMaxWidth: CGFloat = 1040

    // two columns + spacer fit
    static func fitsTwoColumns(_ width: CGFloat, minColumnWidth: CGFloat = columnMinWidth) -> Bool {
        width >= minColumnWidth * 2 + paneSpacing
    }

    // list pane + detail pane fit
    static func fitsListDetail(_ width: CGFloat) -> Bool {
        width >= listPaneWidth + paneSpacing + detailPaneMinWidth
    }
}

extension View {
    // width on every change (unfold, rotate, Split View, resize)
    func onWidthChange(_ action: @escaping (CGFloat) -> Void) -> some View {
        onGeometryChange(for: CGFloat.self) { proxy in
            proxy.size.width
        } action: { width in
            action(width)
        }
    }
}

// first column gets firstFraction of the width
struct AdaptiveTwoColumn<First: View, Second: View>: View {
    var firstFraction: CGFloat = 0.5
    @ViewBuilder var first: First
    @ViewBuilder var second: Second

    var body: some View {
        // Layout, not GeometryReader, which would take all the height
        FractionalHStack(firstFraction: firstFraction, spacing: AdaptiveDimens.paneSpacing) {
            first
            second
        }
    }
}

// exactly two subviews, first gets firstFraction of the width minus spacing
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

// list/detail selection kept across one pane (pushed) <-> two panes (side)
struct ListDetailSelection<ID: Hashable>: Equatable {
    // two panes: side detail
    var selected: ID?
    // one pane: nav stack
    var path: [ID] = []

    // move the open detail between side pane and nav stack
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
