package com.mymovie.log.presentation.adaptive

import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Window-size / aspect-ratio / posture decisions. The windows are generic sizes that stand for
 * kinds of windows (phone, 10:16 cover screen, 4:3 inner display, tablet, split screen...),
 * not specific devices.
 */
class AdaptiveLayoutInfoTest {

    private fun window(width: Int, height: Int, isTabletop: Boolean = false, hinges: List<HingeInfo> = emptyList()) =
        AdaptiveLayoutInfo(width.dp, height.dp, isTabletop, hinges)

    // Search: list 360 + spacing 24 + detail 360, shown next to the navigation rail
    private fun AdaptiveLayoutInfo.searchPanes() =
        paneCount(AdaptiveDimens.ListPaneWidth, AdaptiveDimens.DetailPaneMinWidth)

    // Calendar: 336 + 300 (+ 400 on Large), shown without the navigation rail
    private fun AdaptiveLayoutInfo.calendarPanes() =
        paneCount(336.dp, 300.dp, extraPaneMinWidth = 400.dp, hasNavigationRail = false)

    @Test
    fun phonePortrait_isCompactWithBottomBarAndSinglePane() {
        val info = window(411, 891)
        assertEquals(WindowWidthClass.Compact, info.widthClass)
        // Expanded height starts at 900dp
        assertEquals(WindowHeightClass.Medium, info.heightClass)
        assertEquals(NavigationLayout.BottomBar, info.navigationLayout)
        assertEquals(1, info.searchPanes())
        assertEquals(1, info.calendarPanes())
        assertFalse(info.isLandscape)
    }

    @Test
    fun phoneLandscape_usesRailAndTwoPanes() {
        val info = window(891, 411)
        assertEquals(WindowWidthClass.Expanded, info.widthClass)
        assertEquals(WindowHeightClass.Compact, info.heightClass)
        assertEquals(NavigationLayout.Rail, info.navigationLayout)
        assertEquals(811.dp, info.contentWidth)
        assertEquals(2, info.searchPanes())
        assertTrue(info.isLandscape)
    }

    @Test
    fun flipLikeTallPortrait_staysPhoneLayout() {
        val info = window(412, 1004)
        assertEquals(WindowWidthClass.Compact, info.widthClass)
        assertEquals(NavigationLayout.BottomBar, info.navigationLayout)
        assertEquals(1, info.searchPanes())
    }

    @Test
    fun coverScreen10by16_isCompactAndNotSquare() {
        val info = window(400, 640)
        assertEquals(WindowWidthClass.Compact, info.widthClass)
        assertEquals(1.6f, info.aspectRatio, 0.01f)
        assertFalse(info.isNearSquare)
        assertEquals(1, info.searchPanes())
    }

    @Test
    fun innerDisplay4by3_isExpandedButNearSquare_twoPanesNotThree() {
        val info = window(880, 660)
        assertEquals(WindowWidthClass.Expanded, info.widthClass)
        assertTrue(info.isNearSquare)
        assertEquals(NavigationLayout.Rail, info.navigationLayout)
        // 800dp next to the rail: list 360 + 24 + detail 416
        assertEquals(2, info.searchPanes())
        // Not a Large window, so never three panes — even though it is "a big foldable"
        assertEquals(2, info.calendarPanes())
    }

    @Test
    fun innerDisplay3by4_isMediumAndKeepsDetailWidth() {
        val info = window(660, 880)
        assertEquals(WindowWidthClass.Medium, info.widthClass)
        assertTrue(info.isNearSquare)
        assertEquals(NavigationLayout.Rail, info.navigationLayout)
        // 580dp next to the rail cannot fit list + detail without squeezing them
        assertEquals(1, info.searchPanes())
        // Calendar has no rail and smaller panes: 336 + 24 + 300 = 660
        assertEquals(2, info.calendarPanes())
    }

    @Test
    fun bookStyleInnerDisplay_withoutHinge_isSinglePaneMedium() {
        val info = window(690, 829)
        assertEquals(WindowWidthClass.Medium, info.widthClass)
        assertEquals(1, info.searchPanes())
    }

    @Test
    fun separatingVerticalHinge_becomesPaneBoundary() {
        val hinge = HingeInfo(
            bounds = Rect(900f, 0f, 900f, 2000f),
            isFlat = false,
            isVertical = true,
            isSeparating = true,
            isOccluding = false
        )
        val info = window(690, 829, hinges = listOf(hinge))
        assertTrue(info.isBookPosture)
        assertEquals(listOf(hinge.bounds), info.separatingVerticalHingeBounds)
        assertEquals(2, info.searchPanes())
    }

    @Test
    fun flatFoldWithoutOcclusion_doesNotForcePanes() {
        val flatFold = HingeInfo(
            bounds = Rect(900f, 0f, 900f, 2000f),
            isFlat = true,
            isVertical = true,
            isSeparating = false,
            isOccluding = false
        )
        val info = window(690, 829, hinges = listOf(flatFold))
        assertFalse(info.isBookPosture)
        assertTrue(info.verticalFoldBounds.isEmpty())
        assertEquals(1, info.searchPanes())
    }

    @Test
    fun tabletop_keepsBottomBarAndReportsHorizontalFold() {
        val hinge = HingeInfo(
            bounds = Rect(0f, 1100f, 2200f, 1100f),
            isFlat = false,
            isVertical = false,
            isSeparating = true,
            isOccluding = false
        )
        val info = window(880, 660, isTabletop = true, hinges = listOf(hinge))
        assertEquals(NavigationLayout.BottomBar, info.navigationLayout)
        assertEquals(listOf(hinge.bounds), info.horizontalFoldBounds)
        assertTrue(info.verticalFoldBounds.isEmpty())
    }

    @Test
    fun tabletLandscape_isLargeAndCalendarGetsThreePanes() {
        val info = window(1280, 800)
        assertEquals(WindowWidthClass.Large, info.widthClass)
        assertEquals(2, info.searchPanes())
        assertEquals(3, info.calendarPanes())
    }

    @Test
    fun desktopWindow_isExtraLarge() {
        val info = window(1700, 1000)
        assertEquals(WindowWidthClass.ExtraLarge, info.widthClass)
        assertEquals(3, info.calendarPanes())
    }

    @Test
    fun splitScreenHalfOfTablet_fallsBackToSinglePane() {
        val info = window(640, 800)
        assertEquals(WindowWidthClass.Medium, info.widthClass)
        assertEquals(1, info.searchPanes())
    }

    @Test
    fun splitScreenOnPhone_isCompactWithShortHeight() {
        val info = window(411, 420)
        assertEquals(WindowWidthClass.Compact, info.widthClass)
        assertEquals(WindowHeightClass.Compact, info.heightClass)
        assertEquals(NavigationLayout.BottomBar, info.navigationLayout)
    }

    @Test
    fun resizingAcrossBreakpoints_changesClassesBothWays() {
        assertEquals(WindowWidthClass.Compact, window(599, 800).widthClass)
        assertEquals(WindowWidthClass.Medium, window(600, 800).widthClass)
        assertEquals(WindowWidthClass.Expanded, window(840, 800).widthClass)
        assertEquals(WindowWidthClass.Large, window(1200, 800).widthClass)
        assertEquals(WindowWidthClass.ExtraLarge, window(1600, 800).widthClass)
        assertEquals(WindowWidthClass.Compact, window(599, 800).widthClass)
    }

    @Test
    fun fitsTwoColumns_usesColumnMinimum() {
        assertFalse(AdaptiveDimens.fitsTwoColumns(623.dp))
        assertTrue(AdaptiveDimens.fitsTwoColumns(624.dp))
    }
}
