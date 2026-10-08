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

/** Width buckets from the Material 3 window size class spec (window-core BREAKPOINTS_V2). */
enum class WindowWidthClass { Compact, Medium, Expanded, Large, ExtraLarge }

enum class WindowHeightClass { Compact, Medium, Expanded }

enum class NavigationLayout { BottomBar, Rail }

/**
 * Snapshot of the window currently assigned to the app.
 *
 * Every layout decision in the app is derived from this object — never from the device model.
 * The values come from the actual window (so split screen, freeform/desktop windows and
 * foldable postures are all handled the same way) and are recomputed whenever the window changes.
 *
 * @param windowWidth width of the app window (not the physical display)
 * @param windowHeight height of the app window
 * @param isTabletop true when a horizontal fold is half-opened (e.g. flip phones in Flex mode)
 * @param hinges folds/hinges reported by Jetpack WindowManager, bounds in window pixels
 */
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

    /** Long side / short side. 1.33 for 4:3 and 3:4, 1.6 for 10:16, ~2.2 for tall phones. */
    val aspectRatio: Float =
        max(windowWidth.value, windowHeight.value) / min(windowWidth.value, windowHeight.value).coerceAtLeast(1f)

    val isLandscape: Boolean = windowWidth > windowHeight

    /**
     * Square-ish windows such as 4:3 / 3:4 foldable inner displays or resized desktop windows.
     * On these a "wide" window is not necessarily a "long" one, so screens must not assume a
     * landscape strip of space.
     */
    val isNearSquare: Boolean = aspectRatio <= NEAR_SQUARE_MAX_ASPECT_RATIO

    /** Vertical folds that split content (half-opened book posture or a physical gap/occlusion). */
    val separatingVerticalHingeBounds: List<Rect> =
        hinges.filter { it.isVertical && it.isSeparating }.map { it.bounds }

    /** Vertical folds content must not be placed across (separating or occluding). */
    val verticalFoldBounds: List<Rect> =
        hinges.filter { it.isVertical && (it.isSeparating || it.isOccluding) }.map { it.bounds }

    /** Horizontal folds content must not be placed across (e.g. tabletop / Flex mode). */
    val horizontalFoldBounds: List<Rect> =
        hinges.filter { !it.isVertical && (it.isSeparating || it.isOccluding) }.map { it.bounds }

    val isBookPosture: Boolean = !isTabletop && hinges.any { it.isVertical && !it.isFlat }

    /**
     * Compact windows keep the existing bottom NavigationBar. Anything wider uses a rail so the
     * content keeps its height (landscape phones, unfolded foldables, tablets, desktop windows).
     * In tabletop posture the bar stays at the bottom so it lives on the lower half of the fold,
     * matching the Material navigation-suite default.
     */
    val navigationLayout: NavigationLayout =
        if (isTabletop || widthClass == WindowWidthClass.Compact) NavigationLayout.BottomBar
        else NavigationLayout.Rail

    /** Width actually left for screen content after the navigation rail. */
    val contentWidth: Dp =
        if (navigationLayout == NavigationLayout.Rail) windowWidth - AdaptiveDimens.NavigationRailWidth
        else windowWidth

    fun isWidthAtLeast(widthClass: WindowWidthClass): Boolean = this.widthClass >= widthClass

    /**
     * How many side-by-side panes a list/detail(/extra) screen should show.
     *
     * The window size class is the gate (compact is always one pane, three panes need a
     * Large window) and the width left after the navigation rail must actually fit every pane —
     * so a 4:3 inner display or a Medium window with a rail never gets panes squeezed below
     * their minimum width. A separating vertical hinge is used as a natural pane boundary.
     *
     * @param hasNavigationRail pass false for screens shown without the navigation suite
     */
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
        /** 4:3 is 1.33, 16:10 is 1.6; anything up to 3:2 is treated as "square-ish". */
        const val NEAR_SQUARE_MAX_ASPECT_RATIO = 1.5f

        /** Used before the real window is known (and as the preview/test default). */
        val PhonePortrait = AdaptiveLayoutInfo(windowWidth = 411.dp, windowHeight = 891.dp)
    }
}
