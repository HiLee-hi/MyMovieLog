package com.mymovie.log.presentation.adaptive

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Feeds Jetpack WindowManager FoldingFeatures (the same objects a foldable reports) through
 * rememberAdaptiveLayoutInfo(), i.e. the real currentWindowAdaptiveInfo() pipeline.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class, qualifiers = "w880dp-h660dp")
class FoldingFeaturePostureTest {

    private val publisherRule = WindowLayoutInfoPublisherRule()
    private val composeRule = createAndroidComposeRule<ComponentActivity>()

    @get:Rule
    val rules: TestRule = RuleChain.outerRule(publisherRule).around(composeRule)

    private var info: AdaptiveLayoutInfo? = null

    private fun publish(vararg features: FoldingFeature) {
        composeRule.runOnIdle {
            publisherRule.overrideWindowLayoutInfo(TestWindowLayoutInfo(features.toList()))
        }
        composeRule.waitForIdle()
    }

    private fun start() {
        composeRule.setContent { info = rememberAdaptiveLayoutInfo() }
        composeRule.waitForIdle()
    }

    @Test
    fun noFold_isFlatWindowSizedFromActivity() {
        start()
        publish()
        val current = info!!
        assertFalse(current.isTabletop)
        assertTrue(current.hinges.isEmpty())
        assertEquals(WindowWidthClass.Expanded, current.widthClass)
    }

    @Test
    fun halfOpenedHorizontalFold_isTabletop() {
        start()
        publish(
            FoldingFeature(
                activity = composeRule.activity,
                state = FoldingFeature.State.HALF_OPENED,
                orientation = FoldingFeature.Orientation.HORIZONTAL
            )
        )
        val current = info!!
        assertTrue(current.isTabletop)
        assertEquals(1, current.horizontalFoldBounds.size)
        assertEquals(NavigationLayout.BottomBar, current.navigationLayout)
    }

    @Test
    fun halfOpenedVerticalFold_isBookPostureWithSeparatingHinge() {
        start()
        publish(
            FoldingFeature(
                activity = composeRule.activity,
                state = FoldingFeature.State.HALF_OPENED,
                orientation = FoldingFeature.Orientation.VERTICAL
            )
        )
        val current = info!!
        assertFalse(current.isTabletop)
        assertTrue(current.isBookPosture)
        assertEquals(1, current.separatingVerticalHingeBounds.size)
        // A separating hinge makes list/detail screens use two panes
        assertEquals(2, current.paneCount(AdaptiveDimens.ListPaneWidth, AdaptiveDimens.DetailPaneMinWidth))
    }

    @Test
    fun flatFold_isNotSeparating() {
        start()
        publish(
            FoldingFeature(
                activity = composeRule.activity,
                state = FoldingFeature.State.FLAT,
                orientation = FoldingFeature.Orientation.VERTICAL
            )
        )
        val current = info!!
        assertFalse(current.isBookPosture)
        assertTrue(current.separatingVerticalHingeBounds.isEmpty())
    }
}
