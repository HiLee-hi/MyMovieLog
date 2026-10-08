package com.mymovie.log

import android.graphics.Bitmap
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import kotlin.math.roundToInt

/**
 * Runs the real app on a foldable emulator and changes the actual window while it is running:
 * `wm size` changes the display the app sees (fold to a cover-sized screen, unfold, 4:3, 3:4,
 * tablet) which triggers a real configuration change and Activity recreation, and
 * `cmd device_state` changes the hinge posture reported through Jetpack WindowManager.
 */
@HiltAndroidTest
class FoldableDeviceTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val density get() = instrumentation.targetContext.resources.displayMetrics.densityDpi / 160f

    private fun shell(command: String): String =
        instrumentation.uiAutomation.executeShellCommand(command).let { pfd ->
            android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).bufferedReader().use { it.readText() }
        }

    private fun currentWidthDp(): Int? = runCatching {
        var width: Int? = null
        composeRule.activityRule.scenario.onActivity { width = it.resources.configuration.screenWidthDp }
        width
    }.getOrNull()

    /** Resizes the display in dp (null = physical size) and waits for the recreated Activity. */
    private fun resizeTo(widthDp: Int?, heightDp: Int?) {
        val before = currentWidthDp()
        if (widthDp == null || heightDp == null) {
            shell("wm size reset")
        } else {
            shell("wm size ${(widthDp * density).roundToInt()}x${(heightDp * density).roundToInt()}")
        }
        composeRule.waitUntil(15_000) { currentWidthDp().let { it != null && it != before } }
        composeRule.waitForIdle()
        Thread.sleep(1_000)
        composeRule.waitForIdle()
    }

    private fun setPosture(state: String) {
        shell("cmd device_state state $state")
        Thread.sleep(2_000)
        composeRule.waitForIdle()
    }

    private fun screenshot(name: String) {
        val bitmap = instrumentation.uiAutomation.takeScreenshot() ?: return
        val dir = File(instrumentation.targetContext.getExternalFilesDir(null), "fold_shots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun waitForText(text: String, timeoutMs: Long = 10_000) {
        composeRule.waitUntil(timeoutMs) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun assertEditableText(text: String) {
        composeRule.waitUntil(10_000) {
            composeRule.onAllNodes(hasSetTextAction() and hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Before
    fun setUp() {
        hiltRule.inject()
        shell("wm size reset")
        shell("cmd device_state state reset")
        shell("settings put system accelerometer_rotation 0")
        shell("settings put system user_rotation 0")
        composeRule.waitForIdle()
    }

    @After
    fun tearDown() {
        shell("wm size reset")
        shell("cmd device_state state reset")
        shell("settings put system user_rotation 0")
    }

    @Test
    fun searchWriteLog_draftSurvivesFoldUnfoldAndResize() {
        composeRule.onNodeWithText("검색").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("영화")
        waitForText("영화 2")
        composeRule.onNodeWithText("영화 2").performClick()
        waitForText("기록 수정")
        composeRule.onNodeWithText("기록 수정").performClick()
        composeRule.onNode(hasSetTextAction() and hasText("리뷰 2")).performTextReplacement("폴드 테스트 리뷰")
        screenshot("01_unfolded_search_detail_editor")

        // Fold: cover-sized screen, the Activity is recreated
        resizeTo(344, 882)
        assertEditableText("폴드 테스트 리뷰")
        screenshot("02_folded_cover_editor_sheet")

        // Unfold into a 4:3 inner display
        resizeTo(880, 660)
        assertEditableText("폴드 테스트 리뷰")
        screenshot("03_inner_4x3_editor")

        // Large window: the detail pane is wide enough for the inline editor
        resizeTo(1280, 800)
        waitForText("나의 기록")
        assertEditableText("폴드 테스트 리뷰")
        screenshot("04_tablet_inline_editor")

        // Rotated 3:4 and back to the cover screen
        resizeTo(660, 880)
        assertEditableText("폴드 테스트 리뷰")
        screenshot("05_inner_3x4_editor")
        resizeTo(344, 882)
        assertEditableText("폴드 테스트 리뷰")
    }

    @Test
    fun movieDetailRoute_editorMovesFromSheetToInlineOnUnfold() {
        resizeTo(344, 882)
        composeRule.onNodeWithText("검색").performClick()
        composeRule.onNode(hasSetTextAction()).performTextInput("영화")
        waitForText("영화 1")
        composeRule.onNodeWithText("영화 1").performClick()   // single pane: MovieDetail destination
        waitForText("기록 수정")
        composeRule.onNodeWithText("기록 수정").performClick()
        composeRule.onNode(hasSetTextAction() and hasText("리뷰 1")).performTextReplacement("커버에서 쓰기 시작")
        screenshot("10_cover_detail_sheet")

        resizeTo(880, 660)
        waitForText("나의 기록")
        assertEditableText("커버에서 쓰기 시작")
        screenshot("11_inner_4x3_detail_inline")

        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
        // Back from MovieDetail returns to the search results with the query intact
        waitForText("영화 2")
    }

    @Test
    fun calendar_threePanesToSheets_keepsSelectionAndDraft() {
        resizeTo(1280, 800)
        composeRule.onNodeWithContentDescription("캘린더").performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodes(hasText("10") and hasClickAction() and isEnabled()).onFirst().performClick()
        waitForText("영화 2")
        composeRule.onNodeWithText("영화 2").performClick()
        composeRule.onNode(hasSetTextAction() and hasText("리뷰 2")).performTextReplacement("캘린더에서 수정 중")
        screenshot("20_tablet_calendar_three_panes")

        resizeTo(880, 660)
        assertEditableText("캘린더에서 수정 중")
        screenshot("21_inner_4x3_calendar_two_panes")

        resizeTo(344, 882)
        assertEditableText("캘린더에서 수정 중")
        screenshot("22_cover_calendar_sheets")
    }

    @Test
    fun navigation_barToRail_keepsDestination() {
        resizeTo(344, 882)
        composeRule.onNodeWithText("라이브러리").performClick()
        waitForText("감상 완료 (4)")
        resizeTo(880, 660)
        waitForText("감상 완료 (4)")
        screenshot("30_inner_4x3_library_rail")
        resizeTo(344, 882)
        waitForText("감상 완료 (4)")
        composeRule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }
        waitForText("최근에 본 영화")
    }
}
