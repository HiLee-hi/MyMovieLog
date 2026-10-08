package com.mymovie.log.presentation.adaptive

import androidx.compose.ui.geometry.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ColumnSplitTest {

    @Test
    fun narrowLayout_stacks() {
        val split = computeColumnSplit(0f, layoutWidth = 600, minColumnWidth = 300, spacing = 24, firstFraction = 0.5f, verticalFolds = emptyList())
        assertFalse(split.sideBySide)
    }

    @Test
    fun wideLayout_splitsByFraction() {
        val split = computeColumnSplit(0f, layoutWidth = 1024, minColumnWidth = 300, spacing = 24, firstFraction = 0.5f, verticalFolds = emptyList())
        assertTrue(split.sideBySide)
        assertEquals(500, split.firstWidth)
        assertEquals(524, split.secondOffset)
        assertEquals(500, split.secondWidth)
    }

    @Test
    fun fraction_neverSqueezesAColumnBelowMinimum() {
        val split = computeColumnSplit(0f, layoutWidth = 700, minColumnWidth = 300, spacing = 24, firstFraction = 0.8f, verticalFolds = emptyList())
        assertTrue(split.sideBySide)
        assertEquals(376, split.firstWidth)
        assertEquals(300, split.secondWidth)
    }

    @Test
    fun zeroWidthFold_isCenteredInTheGap() {
        // Layout starts at x=100 in the window; the fold is at x=600
        val fold = Rect(600f, 0f, 600f, 2000f)
        val split = computeColumnSplit(100f, layoutWidth = 1000, minColumnWidth = 300, spacing = 24, firstFraction = 0.7f, verticalFolds = listOf(fold))
        assertTrue(split.sideBySide)
        assertEquals(488, split.firstWidth)   // ends at 588 in window, before the fold
        assertEquals(512, split.secondOffset) // starts at 612 in window, after the fold
        assertEquals(488, split.secondWidth)
    }

    @Test
    fun wideHinge_isUsedAsTheGap() {
        val hinge = Rect(480f, 0f, 540f, 2000f)
        val split = computeColumnSplit(0f, layoutWidth = 1020, minColumnWidth = 300, spacing = 24, firstFraction = 0.5f, verticalFolds = listOf(hinge))
        assertTrue(split.sideBySide)
        assertEquals(480, split.firstWidth)
        assertEquals(540, split.secondOffset)
    }

    @Test
    fun foldNearTheEdge_stacksInsteadOfSqueezing() {
        val fold = Rect(200f, 0f, 200f, 2000f)
        val split = computeColumnSplit(0f, layoutWidth = 1000, minColumnWidth = 300, spacing = 24, firstFraction = 0.5f, verticalFolds = listOf(fold))
        assertFalse(split.sideBySide)
    }

    @Test
    fun foldOutsideTheLayout_isIgnored() {
        val fold = Rect(2000f, 0f, 2000f, 2000f)
        val split = computeColumnSplit(0f, layoutWidth = 1024, minColumnWidth = 300, spacing = 24, firstFraction = 0.5f, verticalFolds = listOf(fold))
        assertTrue(split.sideBySide)
        assertEquals(500, split.firstWidth)
    }

    @Test
    fun horizontalFold_splitsAboveAndBelowHinge() {
        assertEquals(1000 to 1040, computeFoldSplit(layoutTopInWindow = 100f, layoutHeight = 2000, foldTop = 1100f, foldBottom = 1140f))
    }

    @Test
    fun horizontalFoldOutsideLayout_returnsNull() {
        assertNull(computeFoldSplit(layoutTopInWindow = 0f, layoutHeight = 800, foldTop = 900f, foldBottom = 900f))
        assertNull(computeFoldSplit(layoutTopInWindow = 1000f, layoutHeight = 800, foldTop = 900f, foldBottom = 900f))
    }
}
