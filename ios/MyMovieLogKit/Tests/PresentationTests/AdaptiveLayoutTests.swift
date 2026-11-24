@testable import Presentation
import Testing

struct AdaptiveDimensTests {
    @Test func phoneWidthsStayOneColumn() {
        // iPhone 18 Pro Max portrait and the iPhone Duo outer display
        #expect(AdaptiveDimens.fitsTwoColumns(440 - 32) == false)
        #expect(AdaptiveDimens.fitsListDetail(466) == false)
    }

    @Test func unfoldedDisplaysSplit() {
        // iPhone Duo inner display (890 pt) and an iPad in portrait (820 pt)
        #expect(AdaptiveDimens.fitsTwoColumns(890 - 32))
        #expect(AdaptiveDimens.fitsListDetail(890))
        #expect(AdaptiveDimens.fitsListDetail(820))
    }

    @Test func detailPaneBesideSearchStaysCompact() {
        // The detail pane next to a 360 pt result list is too narrow for two columns
        #expect(AdaptiveDimens.fitsTwoColumns(890 - 361 - 64) == false)
    }
}

struct ListDetailSelectionTests {
    @Test func opensInPaneOrPushes() {
        var selection = ListDetailSelection<Int>()
        selection.open(1, isTwoPane: true)
        #expect(selection.selected == 1 && selection.path.isEmpty)

        var compact = ListDetailSelection<Int>()
        compact.open(2, isTwoPane: false)
        #expect(compact.selected == nil && compact.path == [2])
    }

    @Test func foldingPushesTheOpenDetail() {
        var selection = ListDetailSelection<Int>(selected: 7)
        selection.adapt(isTwoPane: false)
        #expect(selection.selected == nil)
        #expect(selection.path == [7])
    }

    @Test func unfoldingMovesThePushedDetailBesideTheList() {
        var selection = ListDetailSelection<Int>(path: [3, 7])
        selection.adapt(isTwoPane: true)
        #expect(selection.selected == 7)
        #expect(selection.path.isEmpty)
    }

    @Test func nothingOpenStaysEmpty() {
        var selection = ListDetailSelection<Int>()
        selection.adapt(isTwoPane: false)
        selection.adapt(isTwoPane: true)
        #expect(selection == ListDetailSelection<Int>())
    }
}
