package com.mymovie.log

import android.graphics.Bitmap
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import org.junit.Assume.assumeTrue
import androidx.test.platform.app.InstrumentationRegistry
import androidx.window.layout.FoldingFeature
import androidx.window.testing.layout.FoldingFeature
import androidx.window.testing.layout.TestWindowLayoutInfo
import androidx.window.testing.layout.WindowLayoutInfoPublisherRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/**
 * Half-opened postures on the real app. This emulator image never reports HALF_OPENED through
 * WindowManager (its device-state to posture mapping always reports FLAT), so the official
 * WindowLayoutInfoPublisherRule publishes the FoldingFeature a half-opened device would report;
 * everything after that (currentWindowAdaptiveInfo -> AdaptiveLayoutInfo -> screens) is real.
 */
@HiltAndroidTest
class PostureDeviceTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val publisherRule = WindowLayoutInfoPublisherRule()

    @get:Rule(order = 2)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()

    private fun shell(command: String) =
        android.os.ParcelFileDescriptor.AutoCloseInputStream(instrumentation.uiAutomation.executeShellCommand(command))
            .bufferedReader().use { it.readText() }

    private fun screenshot(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "fold_shots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun publishHalfOpened(orientation: FoldingFeature.Orientation) {
        composeRule.runOnIdle {
            lateinit var feature: FoldingFeature
            composeRule.activityRule.scenario.onActivity {
                feature = FoldingFeature(activity = it, state = FoldingFeature.State.HALF_OPENED, orientation = orientation)
            }
            publisherRule.overrideWindowLayoutInfo(TestWindowLayoutInfo(listOf(feature)))
        }
        composeRule.waitForIdle()
    }

    private fun waitForText(text: String) = composeRule.waitUntil(10_000) {
        composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }

    private fun closeSoftKeyboard() {
        Espresso.closeSoftKeyboard()
        composeRule.waitForIdle()
    }

    private fun windowSize(): Pair<Int, Int> {
        var size = 0 to 0
        composeRule.activityRule.scenario.onActivity { size = it.window.decorView.width to it.window.decorView.height }
        return size
    }

    @Before
    fun setUp() {
        hiltRule.inject()
        shell("wm size reset")
        shell("pm grant ${instrumentation.targetContext.packageName} android.permission.CAMERA")
    }

    @After
    fun tearDown() {
        shell("settings put system user_rotation 0")
    }

    @Test
    fun tabletop_cameraControlsSitBelowTheHinge_andNavigationStaysAtTheBottom() {
        // Landscape: the fold runs horizontally across the middle (flip / fold in Flex mode)
        shell("settings put system accelerometer_rotation 0")
        shell("settings put system user_rotation 1")
        composeRule.waitUntil(15_000) { windowSize().let { it.first > it.second } }
        composeRule.waitForIdle()
        publishHalfOpened(FoldingFeature.Orientation.HORIZONTAL)

        composeRule.onNodeWithText("검색").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("영화")
        closeSoftKeyboard()
        waitForText("영화 1")
        screenshot("40_tabletop_search")
        composeRule.onNodeWithText("영화 1").performClick()
        // Detail opens in the pane next to the list; start the record editor
        waitForText("기록 수정")
        composeRule.onNodeWithText("기록 수정").performClick()
        closeSoftKeyboard()
        // Short landscape window: the camera button is further down the sheet. performScrollTo()
        // cannot reach it inside the sheet's dialog window, so scroll with a real swipe.
        val (swipeW, swipeH) = windowSize()
        shell("input swipe ${swipeW / 2} ${swipeH * 5 / 6} ${swipeW / 2} ${swipeH / 6} 400")
        Thread.sleep(1_000)
        composeRule.onNodeWithContentDescription("카메라").performClick()
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasContentDescription("닫기")).fetchSemanticsNodes().isNotEmpty()
        }
        publishHalfOpened(FoldingFeature.Orientation.HORIZONTAL)
        Thread.sleep(2_000)
        screenshot("41_tabletop_camera")

        val closeTop = composeRule.onNodeWithContentDescription("닫기").fetchSemanticsNode().boundsInWindow.top
        val height = windowSize().second
        assertTrue("controls must start below the hinge: top=$closeTop height=$height", closeTop > height / 2f)
    }

    @Test
    fun bookPosture_searchPanesMeetAtTheHinge() {
        // Book posture only splits windows of Medium width or more (a vertical fold on a
        // compact window would leave two unusably narrow panes)
        var widthDp = 0
        composeRule.activityRule.scenario.onActivity { widthDp = it.resources.configuration.screenWidthDp }
        assumeTrue("needs a Medium or wider window, was ${widthDp}dp", widthDp >= 600)

        publishHalfOpened(FoldingFeature.Orientation.VERTICAL)
        composeRule.onNodeWithText("검색").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("영화")
        closeSoftKeyboard()
        waitForText("영화 1")
        composeRule.onNodeWithText("영화 1").performClick()
        waitForText("기록 수정")
        screenshot("42_book_posture_search")

        val hingeX = windowSize().first / 2f
        val listItemRight = composeRule.onNodeWithText("영화 2").fetchSemanticsNode().boundsInWindow.right
        val detailLeft = composeRule.onNodeWithText("기록 수정").fetchSemanticsNode().boundsInWindow.left
        assertTrue("list must end before the hinge: $listItemRight < $hingeX", listItemRight <= hingeX)
        assertTrue("detail must start after the hinge: $detailLeft > $hingeX", detailLeft >= hingeX)
    }
}
