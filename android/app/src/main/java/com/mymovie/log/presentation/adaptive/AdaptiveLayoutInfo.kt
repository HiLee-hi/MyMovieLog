package com.mymovie.log.presentation.adaptive

import androidx.compose.material3.adaptive.HingeInfo
import androidx.compose.runtime.Immutable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass
import androidx.window.core.layout.computeWindowSizeClass
import kotlin.math.max
import kotlin.math.min

// M3 window size class widths (window-core BREAKPOINTS_V2)
enum class WindowWidthClass { Compact, Medium, Expanded, Large, ExtraLarge }

enum class WindowHeightClass { Compact, Medium, Expanded }

enum class NavigationLayout { BottomBar, Rail }

// current app window (not the device), recomputed on every window change
// covers split screen, freeform windows and fold postures the same way
@Immutable
data class AdaptiveLayoutInfo(
    val windowWidth: Dp,
    val windowHeight: Dp,
    val isTabletop: Boolean = false,
    val hinges: List<HingeInfo> = emptyList(),
) {
    private val sizeClass: WindowSizeClass =
        WindowSizeClass.BREAKPOINTS_V2.computeWindowSizeClass(
            windowWidth.value.coerceAtLeast(0f),
            windowHeight.value.coerceAtLeast(0f)
        )

    val widthClass: WindowWidthClass = when {
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXTRA_LARGE_LOWER_BOUND) -> WindowWidthClass.ExtraLarge
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_LARGE_LOWER_BOUND) -> WindowWidthClass.Large
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_EXPANDED_LOWER_BOUND) -> WindowWidthClass.Expanded
        sizeClass.isWidthAtLeastBreakpoint(WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND) -> WindowWidthClass.Medium
        else -> WindowWidthClass.Compact
    }

    val heightClass: WindowHeightClass = when {
        sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_EXPANDED_LOWER_BOUND) -> WindowHeightClass.Expanded
        sizeClass.isHeightAtLeastBreakpoint(WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND) -> WindowHeightClass.Medium
        else -> WindowHeightClass.Compact
    }

    // long / short side: 1.33 for 4:3, 1.6 for 10:16, ~2.2 for tall phones
    val aspectRatio: Float =
        max(windowWidth.value, windowHeight.value) / min(windowWidth.value, windowHeight.value).coerceAtLeast(1f)

    val isLandscape: Boolean = windowWidth > windowHeight

    // 4:3 / 3:4 inner displays, resized desktop windows: wide but not long
    val isNearSquare: Boolean = aspectRatio <= NEAR_SQUARE_MAX_ASPECT_RATIO

    // vertical folds that split content (half-opened book, physical gap)
    val separatingVerticalHingeBounds: List<Rect> =
        hinges.filter { it.isVertical && it.isSeparating }.map { it.bounds }

    // vertical folds to keep content off (separating / occluding)
    val verticalFoldBounds: List<Rect> =
        hinges.filter { it.isVertical && (it.isSeparating || it.isOccluding) }.map { it.bounds }

    // horizontal folds to keep content off (tabletop / Flex mode)
    val horizontalFoldBounds: List<Rect> =
        hinges.filter { !it.isVertical && (it.isSeparating || it.isOccluding) }.map { it.bounds }

    val isBookPosture: Boolean = !isTabletop && hinges.any { it.isVertical && !it.isFlat }

    // compact -> bottom bar, wider -> rail to keep content height
    // tabletop keeps the bar at the bottom (lower half), like the navigation-suite default
    val navigationLayout: NavigationLayout =
        if (isTabletop || widthClass == WindowWidthClass.Compact) NavigationLayout.BottomBar
        else NavigationLayout.Rail

    // content width after the rail
    val contentWidth: Dp =
        if (navigationLayout == NavigationLayout.Rail) windowWidth - AdaptiveDimens.NavigationRailWidth
        else windowWidth

    fun isWidthAtLeast(widthClass: WindowWidthClass): Boolean = this.widthClass >= widthClass

    // size class gates the count (compact = 1, 3 panes need Large), then every pane must fit its min width
    // separating vertical hinge = pane boundary
    // hasNavigationRail = false for screens without the navigation suite
    fun paneCount(
        listPaneWidth: Dp,
        detailPaneMinWidth: Dp,
        extraPaneMinWidth: Dp? = null,
        hasNavigationRail: Boolean = true,
    ): Int {
        if (widthClass == WindowWidthClass.Compact) return 1
        val available = if (hasNavigationRail) contentWidth else windowWidth
        val spacing = AdaptiveDimens.PaneSpacing
        var panes = if (available >= listPaneWidth + spacing + detailPaneMinWidth) 2 else 1
        if (separatingVerticalHingeBounds.isNotEmpty()) panes = max(panes, 2)
        if (
            extraPaneMinWidth != null && panes == 2 &&
            isWidthAtLeast(WindowWidthClass.Large) &&
            available >= listPaneWidth + detailPaneMinWidth + extraPaneMinWidth + spacing * 2
        ) {
            panes = 3
        }
        return panes
    }

    companion object {
        // up to 3:2 counts as square-ish (4:3 = 1.33, 16:10 = 1.6)
        const val NEAR_SQUARE_MAX_ASPECT_RATIO = 1.5f

        // before the real window is known, also preview / test default
        val PhonePortrait = AdaptiveLayoutInfo(windowWidth = 411.dp, windowHeight = 891.dp)
    }
}
