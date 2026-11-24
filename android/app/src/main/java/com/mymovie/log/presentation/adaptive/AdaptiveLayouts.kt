package com.mymovie.log.presentation.adaptive

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.PaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// centered, width capped for tablet / 4:3 / maximized windows
fun Modifier.constrainedWidth(maxWidth: Dp = AdaptiveDimens.ContentMaxWidth): Modifier =
    this
        .fillMaxWidth()
        .wrapContentWidth(Alignment.CenterHorizontally)
        .widthIn(max = maxWidth)
        .fillMaxWidth()

// pane count from AdaptiveLayoutInfo so the rail and per-screen min widths count
// separating vertical hinges excluded, like HingePolicy.AvoidSeparating
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
fun AdaptiveLayoutInfo.paneScaffoldDirective(
    maxPanes: Int,
    preferredPaneWidth: Dp = AdaptiveDimens.ListPaneWidth,
): PaneScaffoldDirective = PaneScaffoldDirective(
    maxHorizontalPartitions = maxPanes,
    horizontalPartitionSpacerSize = if (maxPanes > 1) AdaptiveDimens.PaneSpacing else 0.dp,
    maxVerticalPartitions = if (isTabletop) 2 else 1,
    verticalPartitionSpacerSize = if (isTabletop) AdaptiveDimens.PaneSpacing else 0.dp,
    defaultPanePreferredWidth = preferredPaneWidth,
    // single pane would leave one side of the fold empty
    excludedBounds = if (maxPanes > 1) separatingVerticalHingeBounds else emptyList()
)

internal data class ColumnSplit(
    val sideBySide: Boolean,
    val firstWidth: Int = 0,
    val secondOffset: Int = 0,
    val secondWidth: Int = 0,
)

// two columns in layoutWidth px? a vertical fold inside the layout becomes the boundary
internal fun computeColumnSplit(
    layoutLeftInWindow: Float,
    layoutWidth: Int,
    minColumnWidth: Int,
    spacing: Int,
    firstFraction: Float,
    verticalFolds: List<Rect>,
): ColumnSplit {
    if (layoutWidth < minColumnWidth * 2 + spacing) return ColumnSplit(sideBySide = false)

    val layoutRight = layoutLeftInWindow + layoutWidth
    val fold = verticalFolds.firstOrNull { it.center.x > layoutLeftInWindow && it.center.x < layoutRight }
    if (fold != null) {
        val center = fold.center.x
        val gapStart = min(fold.left, center - spacing / 2f) - layoutLeftInWindow
        val gapEnd = max(fold.right, center + spacing / 2f) - layoutLeftInWindow
        val firstWidth = gapStart.toInt()
        val secondOffset = ceil(gapEnd).toInt()
        val secondWidth = layoutWidth - secondOffset
        return if (firstWidth >= minColumnWidth && secondWidth >= minColumnWidth) {
            ColumnSplit(true, firstWidth, secondOffset, secondWidth)
        } else {
            ColumnSplit(sideBySide = false)
        }
    }

    val firstWidth = ((layoutWidth - spacing) * firstFraction).roundToInt()
        .coerceIn(minColumnWidth, layoutWidth - spacing - minColumnWidth)
    return ColumnSplit(true, firstWidth, firstWidth + spacing, layoutWidth - firstWidth - spacing)
}

// side by side if the available width fits, else stacked
// both slots stay in composition, so field / scroll state survives fold, rotate, resize
// for scrolling content: stacked height = sum of both
@Composable
fun AdaptiveTwoColumn(
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    firstFraction: Float = 0.5f,
    minColumnWidth: Dp = AdaptiveDimens.ColumnMinWidth,
    spacing: Dp = AdaptiveDimens.PaneSpacing,
) {
    val folds = LocalAdaptiveLayoutInfo.current.verticalFoldBounds
    var layoutLeftInWindow by remember { mutableFloatStateOf(0f) }

    Layout(
        contents = listOf(first, second),
        modifier = if (folds.isEmpty()) modifier
        else modifier.onPlaced { layoutLeftInWindow = it.positionInWindow().x }
    ) { (firstMeasurables, secondMeasurables), constraints ->
        val spacingPx = spacing.roundToPx()
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else constraints.minWidth
        val split = computeColumnSplit(
            layoutLeftInWindow = layoutLeftInWindow,
            layoutWidth = width,
            minColumnWidth = minColumnWidth.roundToPx(),
            spacing = spacingPx,
            firstFraction = firstFraction,
            verticalFolds = folds
        )

        if (split.sideBySide) {
            val firstPlaceables = firstMeasurables.map {
                it.measure(Constraints.fixedWidth(split.firstWidth).copy(maxHeight = constraints.maxHeight))
            }
            val secondPlaceables = secondMeasurables.map {
                it.measure(Constraints.fixedWidth(split.secondWidth).copy(maxHeight = constraints.maxHeight))
            }
            val height = max(firstPlaceables.totalHeight(), secondPlaceables.totalHeight())
                .coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(width, height) {
                placeStacked(firstPlaceables, x = 0)
                placeStacked(secondPlaceables, x = split.secondOffset)
            }
        } else {
            val childConstraints = Constraints.fixedWidth(width)
            val firstPlaceables = firstMeasurables.map { it.measure(childConstraints) }
            val secondPlaceables = secondMeasurables.map { it.measure(childConstraints) }
            val firstHeight = firstPlaceables.totalHeight()
            val gap = if (firstHeight > 0 && secondPlaceables.totalHeight() > 0) spacingPx else 0
            val height = (firstHeight + gap + secondPlaceables.totalHeight())
                .coerceIn(constraints.minHeight, constraints.maxHeight)
            layout(width, height) {
                placeStacked(firstPlaceables, x = 0)
                placeStacked(secondPlaceables, x = 0, startY = firstHeight + gap)
            }
        }
    }
}

// controls over content, tabletop -> content above the hinge, controls below
// slots keep their composition place, so the camera AndroidView isn't re-parented
@Composable
fun TabletopAwareLayout(
    content: @Composable () -> Unit,
    controls: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val info = LocalAdaptiveLayoutInfo.current
    val fold = if (info.isTabletop) info.horizontalFoldBounds.firstOrNull() else null
    var layoutTopInWindow by remember { mutableFloatStateOf(0f) }

    Layout(
        contents = listOf(content, controls),
        modifier = if (fold == null) modifier
        else modifier.onPlaced { layoutTopInWindow = it.positionInWindow().y }
    ) { (contentMeasurables, controlMeasurables), constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val split = fold?.let {
            computeFoldSplit(layoutTopInWindow, height, it.top, it.bottom)
        }
        if (split == null) {
            val full = Constraints.fixed(width, height)
            val contentPlaceables = contentMeasurables.map { it.measure(full) }
            val controlPlaceables = controlMeasurables.map { it.measure(full) }
            layout(width, height) {
                contentPlaceables.forEach { it.place(0, 0) }
                controlPlaceables.forEach { it.place(0, 0) }
            }
        } else {
            val contentPlaceables = contentMeasurables.map { it.measure(Constraints.fixed(width, split.first)) }
            val controlPlaceables = controlMeasurables.map {
                it.measure(Constraints.fixed(width, height - split.second))
            }
            layout(width, height) {
                contentPlaceables.forEach { it.place(0, 0) }
                controlPlaceables.forEach { it.place(0, split.second) }
            }
        }
    }
}

// (height above fold, y where below starts) in px, null if the fold misses the layout
internal fun computeFoldSplit(layoutTopInWindow: Float, layoutHeight: Int, foldTop: Float, foldBottom: Float): Pair<Int, Int>? {
    val top = (foldTop - layoutTopInWindow).roundToInt()
    val bottom = (foldBottom - layoutTopInWindow).roundToInt().coerceAtLeast(top)
    if (top <= 0 || bottom >= layoutHeight) return null
    return top to bottom
}

private fun List<Placeable>.totalHeight(): Int = sumOf { it.height }

private fun Placeable.PlacementScope.placeStacked(placeables: List<Placeable>, x: Int, startY: Int = 0) {
    var y = startY
    placeables.forEach {
        it.placeRelative(x, y)
        y += it.height
    }
}
