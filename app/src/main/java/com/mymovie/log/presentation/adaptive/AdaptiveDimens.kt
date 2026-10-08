package com.mymovie.log.presentation.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared layout measurements for adaptive screens.
 *
 * Window-level decisions come from [AdaptiveLayoutInfo]; these values only describe how much
 * room a piece of content needs, so every screen reflows against the same numbers instead of
 * scattering width checks.
 */
object AdaptiveDimens {
    /** Material 3 spacer between panes / columns. */
    val PaneSpacing: Dp = 24.dp

    /** Material 3 NavigationRail container width. */
    val NavigationRailWidth: Dp = 80.dp

    /** Preferred width of a list pane (search results, calendar). */
    val ListPaneWidth: Dp = 360.dp

    /** Smallest width a detail pane may shrink to before we fall back to a single pane. */
    val DetailPaneMinWidth: Dp = 360.dp

    /** Smallest width of the record detail (third) pane on Large windows. */
    val ExtraPaneMinWidth: Dp = 360.dp

    /** A column inside a screen (e.g. Overview | My log) is never narrower than this. */
    val ColumnMinWidth: Dp = 300.dp

    /** Long text is capped to a comfortable reading measure. */
    val ReadableMaxWidth: Dp = 640.dp

    /** Whole-screen content is capped so cards do not stretch across a tablet or 4:3 display. */
    val ContentMaxWidth: Dp = 1040.dp

    /** Library / picker grid cells. */
    val PosterGridMinCellWidth: Dp = 108.dp
    val PhotoGridMinCellWidth: Dp = 112.dp

    /** True when [availableWidth] fits two [ColumnMinWidth] columns plus the spacer. */
    fun fitsTwoColumns(availableWidth: Dp, minColumnWidth: Dp = ColumnMinWidth): Boolean =
        availableWidth >= minColumnWidth * 2 + PaneSpacing
}
