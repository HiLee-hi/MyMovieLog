package com.mymovie.log.presentation.adaptive

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// content size thresholds, window-level decisions live in AdaptiveLayoutInfo
object AdaptiveDimens {
    // M3 spacer between panes / columns
    val PaneSpacing: Dp = 24.dp

    // M3 NavigationRail width
    val NavigationRailWidth: Dp = 80.dp

    // list pane width (search results, calendar)
    val ListPaneWidth: Dp = 360.dp

    // below this -> single pane
    val DetailPaneMinWidth: Dp = 360.dp

    // min width of the record (third) pane on Large
    val ExtraPaneMinWidth: Dp = 360.dp

    // min width of a column inside a screen (e.g. overview | my log)
    val ColumnMinWidth: Dp = 300.dp

    // reading width cap for long text
    val ReadableMaxWidth: Dp = 640.dp

    // content width cap so cards don't stretch on tablet / 4:3
    val ContentMaxWidth: Dp = 1040.dp

    // library / picker grid cell
    val PosterGridMinCellWidth: Dp = 108.dp
    val PhotoGridMinCellWidth: Dp = 112.dp

    // two ColumnMinWidth columns + spacer fit
    fun fitsTwoColumns(availableWidth: Dp, minColumnWidth: Dp = ColumnMinWidth): Boolean =
        availableWidth >= minColumnWidth * 2 + PaneSpacing
}
