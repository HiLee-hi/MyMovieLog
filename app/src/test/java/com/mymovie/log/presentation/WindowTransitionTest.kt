package com.mymovie.log.presentation

import android.app.Application
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mymovie.log.presentation.adaptive.AdaptiveTwoColumn
import com.mymovie.log.presentation.adaptive.LocalAdaptiveLayoutInfo
import com.mymovie.log.presentation.adaptive.NavigationLayout
import com.mymovie.log.presentation.calendar.CalendarScreen
import com.mymovie.log.presentation.calendar.CalendarTestTags
import com.mymovie.log.presentation.detail.MovieDetailScreen
import com.mymovie.log.presentation.search.SearchScreen
import com.mymovie.log.presentation.search.SearchTestTags
import com.mymovie.log.testing.TestViewModels
import com.mymovie.log.testing.TestWindow
import com.mymovie.log.testing.WindowSpec
import com.mymovie.log.testing.WindowSpecs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.Duration

/**
 * Runtime window changes: fold/unfold, rotation, split screen and resizing all change the window
 * size while the screen stays alive. The window is switched while the content is composed, then
 * we check that the layout reflows and the user's state is still there.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w1400dp-h1100dp-mdpi")
class WindowTransitionTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var window by mutableStateOf(WindowSpecs.PhonePortrait)

    private fun resizeTo(spec: WindowSpec) {
        window = spec
        composeRule.waitForIdle()
    }

    private fun pressBack() {
        composeRule.runOnUiThread { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun writeLogDraft_movesBetweenInlinePaneAndSheet_withoutLosingInput() {
        val viewModel = TestViewModels.movieDetail(movieId = 1)
        window = WindowSpecs.Inner4x3
        composeRule.setContent {
            TestWindow(window) { MovieDetailScreen(onBack = {}, viewModel = viewModel) }
        }
        composeRule.onNodeWithText("기록 수정").performClick()
        composeRule.onNode(hasSetTextAction() and hasText("리뷰 1")).performTextReplacement("접었다 펴도 남는 리뷰")
        composeRule.onNodeWithText("나의 기록").assertExists()

        // Unfolded -> folded (cover screen): the same draft is now in the bottom sheet
        resizeTo(WindowSpecs.Cover10x16)
        composeRule.onNodeWithText("나의 기록").assertDoesNotExist()
        composeRule.onNodeWithText("기록 저장").assertExists()
        composeRule.onNode(hasSetTextAction() and hasText("접었다 펴도 남는 리뷰")).assertExists()

        // Rotate / unfold again: back inline, still the same text
        resizeTo(WindowSpecs.PhoneLandscape)
        composeRule.onNodeWithText("나의 기록").assertExists()
        composeRule.onNode(hasSetTextAction() and hasText("접었다 펴도 남는 리뷰")).assertExists()
        assertEquals("접었다 펴도 남는 리뷰", viewModel.recordDraft.value.review)
    }

    @Test
    fun search_selectedMovieSurvivesFold_andBackReturnsToResults() {
        val viewModel = TestViewModels.search(SavedStateHandle(mapOf("search_query" to "영화")))
        window = WindowSpecs.Inner4x3
        composeRule.setContent {
            TestWindow(window, withNavigationSuite = true) {
                SearchScreen(
                    viewModel = viewModel,
                    detailPane = { movieId, isSinglePane, _ ->
                        Text("detail-$movieId single=$isSinglePane")
                    }
                )
            }
        }
        // Let the 400ms query debounce and paging load run
        repeat(20) {
            composeRule.waitForIdle()
            if (composeRule.onAllNodesWithText("영화 2").fetchSemanticsNodes().isNotEmpty()) return@repeat
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        }

        // Two panes: the result opens next to the list
        composeRule.onNodeWithText("영화 2").performClick()
        composeRule.onNodeWithText("detail-2 single=false").assertExists()
        composeRule.onNodeWithTag(SearchTestTags.ListPane).assertExists()

        // Fold to a compact window: the open movie stays on screen, full width
        resizeTo(WindowSpecs.PhonePortrait)
        composeRule.onNodeWithText("detail-2 single=true").assertExists()
        composeRule.onNodeWithTag(SearchTestTags.ListPane).assertDoesNotExist()

        // Back returns to the results, with the query intact
        pressBack()
        composeRule.onNodeWithTag(SearchTestTags.ListPane).assertExists()
        composeRule.onNodeWithText("detail-2", substring = true).assertDoesNotExist()
        assertEquals("영화", viewModel.query.value)

        // Unfold again: two panes, empty detail placeholder next to the list
        resizeTo(WindowSpecs.Inner4x3)
        composeRule.onNodeWithTag(SearchTestTags.DetailPane).assertExists()
        composeRule.onNodeWithTag(SearchTestTags.ListPane).assertExists()
    }

    @Test
    fun calendar_threePanesToSheets_keepsDateRecordAndDraft() {
        val viewModel = TestViewModels.calendar()
        window = WindowSpecs.TabletLandscape
        composeRule.setContent {
            TestWindow(window) { CalendarScreen(onBack = {}, isLoggedIn = true, viewModel = viewModel) }
        }
        composeRule.runOnIdle { viewModel.onDateSelected(TestViewModels.watchedDate) }
        composeRule.onNodeWithText("영화 2").performClick()
        composeRule.onNodeWithTag(CalendarTestTags.RecordDetailPane).assertExists()
        composeRule.onNode(hasSetTextAction() and hasText("리뷰 2")).performTextReplacement("태블릿에서 쓰던 리뷰")

        // Large -> 4:3: third pane goes away, the editor continues in a sheet
        resizeTo(WindowSpecs.Inner4x3)
        composeRule.onNodeWithTag(CalendarTestTags.RecordDetailPane).assertDoesNotExist()
        composeRule.onNodeWithTag(CalendarTestTags.RecordsPane).assertExists()
        composeRule.onNode(hasSetTextAction() and hasText("태블릿에서 쓰던 리뷰")).assertExists()

        // -> compact: one pane, date + record still selected
        resizeTo(WindowSpecs.PhonePortrait)
        composeRule.onNodeWithTag(CalendarTestTags.RecordsPane).assertDoesNotExist()
        composeRule.onNode(hasSetTextAction() and hasText("태블릿에서 쓰던 리뷰")).assertExists()
        assertEquals(TestViewModels.watchedDate, viewModel.selectedDate.value)
        assertEquals("r2", viewModel.selectedRecord.value?.id)
    }

    @Test
    fun navigationBarToRail_keepsBackStack() {
        lateinit var navController: NavHostController
        window = WindowSpecs.PhonePortrait
        composeRule.setContent {
            // Same structure as AppNavHost: NavController above the navigation suite
            navController = rememberNavController()
            TestWindow(window) {
                val info = LocalAdaptiveLayoutInfo.current
                NavigationSuiteScaffold(
                    layoutType = if (info.navigationLayout == NavigationLayout.Rail) {
                        NavigationSuiteType.NavigationRail
                    } else {
                        NavigationSuiteType.NavigationBar
                    },
                    navigationSuiteItems = {
                        item(selected = true, onClick = {}, icon = {}, label = { Text("홈") })
                    }
                ) {
                    NavHost(navController, startDestination = "home") {
                        composable("home") { Text("home screen") }
                        composable("detail") {
                            var taps by remember { mutableIntStateOf(0) }
                            Button(onClick = { taps++ }) { Text("taps=$taps") }
                        }
                    }
                }
            }
        }
        composeRule.runOnIdle { navController.navigate("detail") }
        composeRule.onNodeWithText("taps=0").performClick()
        val entryBefore = composeRule.runOnIdle { navController.currentBackStackEntry }

        resizeTo(WindowSpecs.Inner4x3)      // bar -> rail
        resizeTo(WindowSpecs.PhoneLandscape)
        resizeTo(WindowSpecs.Cover10x16)    // rail -> bar

        composeRule.onNodeWithText("taps=1").assertExists()
        composeRule.runOnIdle {
            assertEquals("detail", navController.currentDestination?.route)
            assertSame(entryBefore, navController.currentBackStackEntry)
        }
        pressBack()
        composeRule.onNodeWithText("home screen").assertExists()
    }

    @Test
    fun twoColumnReflow_keepsSlotState() {
        window = WindowSpecs.TabletLandscape
        composeRule.setContent {
            TestWindow(window) {
                AdaptiveTwoColumn(
                    first = {
                        var count by remember { mutableIntStateOf(0) }
                        Button(onClick = { count++ }) { Text("count=$count") }
                    },
                    second = { Column { Text("second") } }
                )
            }
        }
        composeRule.onNodeWithText("count=0").performClick()
        resizeTo(WindowSpecs.PhonePortrait)   // side by side -> stacked
        resizeTo(WindowSpecs.Inner4x3)        // stacked -> side by side
        composeRule.onNodeWithText("count=1").assertExists()
    }
}
