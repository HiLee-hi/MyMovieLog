package com.mymovie.log.presentation

import android.app.Application
import android.os.Looper
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import com.mymovie.log.domain.usecase.DeleteRecordUseCase
import com.mymovie.log.domain.usecase.GetCurrentUserIdUseCase
import com.mymovie.log.domain.usecase.GetRecordsUseCase
import com.mymovie.log.domain.usecase.GetSignedPhotoUrlsUseCase
import com.mymovie.log.domain.usecase.UploadPhotosUseCase
import com.mymovie.log.domain.usecase.UpsertRecordUseCase
import com.mymovie.log.presentation.calendar.CalendarScreen
import com.mymovie.log.presentation.calendar.CalendarTestTags
import com.mymovie.log.presentation.detail.MovieDetailScreen
import com.mymovie.log.presentation.home.HomeScreen
import com.mymovie.log.presentation.home.HomeTestTags
import com.mymovie.log.presentation.library.LibraryScreen
import com.mymovie.log.presentation.library.LibraryViewModel
import com.mymovie.log.presentation.search.SearchScreen
import com.mymovie.log.presentation.search.SearchTestTags
import com.mymovie.log.presentation.stats.StatsScreen
import com.mymovie.log.presentation.stats.StatsTestTags
import com.mymovie.log.testing.FakeAuthRepository
import com.mymovie.log.testing.FakePhotoRepository
import com.mymovie.log.testing.TestViewModels
import com.mymovie.log.testing.TestWindow
import com.mymovie.log.testing.WindowSpec
import com.mymovie.log.testing.WindowSpecs
import com.mymovie.log.testing.saveWindowScreenshot
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration
import org.robolectric.annotation.GraphicsMode

/**
 * Renders the real screens (real ViewModels over in-memory fakes) in representative windows and
 * checks that each one reflows as designed. Screenshots go to
 * app/build/outputs/adaptive-screenshots for visual review.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "w1400dp-h1100dp-mdpi")
class AdaptiveScreenLayoutTest(private val spec: WindowSpec) {

    @get:Rule
    val composeRule = createComposeRule()

    private data class Expectation(
        val homeTwoColumns: Boolean,
        val statsTwoColumns: Boolean,
        val detailWide: Boolean,
        val calendarPanes: Int,
        val searchPanes: Int,
    )

    private val expected = when (spec) {
        WindowSpecs.PhonePortrait -> Expectation(false, false, false, 1, 1)
        WindowSpecs.PhoneLandscape -> Expectation(true, true, true, 2, 2)
        WindowSpecs.FlipPortrait -> Expectation(false, false, false, 1, 1)
        WindowSpecs.NarrowCover -> Expectation(false, false, false, 1, 1)
        // Content next to the rail is 610dp: one column; detail/calendar have no rail
        WindowSpecs.BookInner -> Expectation(false, false, true, 2, 1)
        WindowSpecs.Cover10x16 -> Expectation(false, false, false, 1, 1)
        // 4:3: two panes / columns, never three
        WindowSpecs.Inner4x3 -> Expectation(true, true, true, 2, 2)
        // 3:4 next to the rail is 580dp: search stays single pane
        WindowSpecs.Inner3x4 -> Expectation(false, false, true, 2, 1)
        WindowSpecs.TabletLandscape -> Expectation(true, true, true, 3, 2)
        else -> error("no expectation for $spec")
    }

    @Test
    fun home() {
        val viewModel = TestViewModels.home()
        composeRule.setContent {
            TestWindow(spec, withNavigationSuite = true) {
                HomeScreen(onNavigateToCalendar = {}, viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.saveWindowScreenshot("home_$spec")
        composeRule.onNodeWithText("최근에 본 영화").assertExistsIf(true)
        composeRule.onNodeWithTag(HomeTestTags.TwoColumn).assertExistsIf(expected.homeTwoColumns)
    }

    @Test
    fun stats() {
        val viewModel = TestViewModels.stats()
        composeRule.setContent {
            TestWindow(spec, withNavigationSuite = true) {
                StatsScreen(isLoggedIn = true, viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.saveWindowScreenshot("stats_$spec")
        composeRule.onNodeWithTag(StatsTestTags.TwoColumn).assertExistsIf(expected.statsTwoColumns)
    }

    @Test
    fun movieDetail() {
        val viewModel = TestViewModels.movieDetail(movieId = 1)
        composeRule.setContent {
            TestWindow(spec) {
                MovieDetailScreen(onBack = {}, viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.saveWindowScreenshot("movie_detail_$spec")
        composeRule.onNodeWithText("나의 기록").assertExistsIf(expected.detailWide)
    }

    @Test
    fun movieDetailWithInlineEditor() {
        val viewModel = TestViewModels.movieDetail(movieId = 1)
        composeRule.setContent {
            TestWindow(spec) {
                MovieDetailScreen(onBack = {}, viewModel = viewModel)
            }
        }
        composeRule.runOnIdle { viewModel.onRecordClick() }
        composeRule.waitForIdle()
        if (expected.detailWide) {
            // On wide windows the editor is inline, next to the movie information
            composeRule.saveWindowScreenshot("write_log_inline_$spec")
            composeRule.onNodeWithText("기록 저장").assertExistsIf(true)
        }
    }

    @Test
    fun calendar() {
        val viewModel = TestViewModels.calendar()
        viewModel.onDateSelected(TestViewModels.watchedDate)
        composeRule.setContent {
            TestWindow(spec) {
                CalendarScreen(onBack = {}, isLoggedIn = true, viewModel = viewModel)
            }
        }
        if (expected.calendarPanes == 3) {
            composeRule.runOnIdle { viewModel.selectRecord(TestViewModels.sampleRecords().first()) }
        }
        composeRule.waitForIdle()
        composeRule.saveWindowScreenshot("calendar_$spec")
        composeRule.onNodeWithTag(CalendarTestTags.RecordsPane).assertExistsIf(expected.calendarPanes >= 2)
        composeRule.onNodeWithTag(CalendarTestTags.RecordDetailPane).assertExistsIf(expected.calendarPanes >= 3)
    }

    @Test
    fun search() {
        val viewModel = TestViewModels.search(SavedStateHandle(mapOf("search_query" to "영화")))
        composeRule.setContent {
            TestWindow(spec, withNavigationSuite = true) {
                SearchScreen(viewModel = viewModel)
            }
        }
        // Let the 400ms query debounce run on the main looper
        repeat(6) {
            composeRule.waitForIdle()
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        }
        composeRule.waitForIdle()
        composeRule.saveWindowScreenshot("search_$spec")
        composeRule.onNodeWithTag(SearchTestTags.ListPane).assertExistsIf(true)
        composeRule.onNodeWithText("영화 2").assertExistsIf(true)
        composeRule.onNodeWithTag(SearchTestTags.DetailPane).assertExistsIf(expected.searchPanes == 2)
    }

    @Test
    fun library() {
        val repo = TestViewModels.recordRepository()
        val viewModel = LibraryViewModel(
            GetRecordsUseCase(repo),
            DeleteRecordUseCase(repo),
            UpsertRecordUseCase(repo),
            UploadPhotosUseCase(FakePhotoRepository()),
            GetSignedPhotoUrlsUseCase(FakePhotoRepository()),
            GetCurrentUserIdUseCase(FakeAuthRepository()),
            SavedStateHandle()
        )
        composeRule.setContent {
            TestWindow(spec, withNavigationSuite = true) {
                LibraryScreen(isLoggedIn = true, viewModel = viewModel)
            }
        }
        composeRule.waitForIdle()
        composeRule.saveWindowScreenshot("library_$spec")
        composeRule.onNodeWithText("감상 완료 (4)").assertExistsIf(true)
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}")
        fun windows(): List<Array<Any>> = WindowSpecs.all.map { arrayOf(it) }
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.assertExistsIf(condition: Boolean) {
    if (condition) assertExists() else assertDoesNotExist()
}
