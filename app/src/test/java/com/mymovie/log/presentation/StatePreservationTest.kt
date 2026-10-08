package com.mymovie.log.presentation

import androidx.lifecycle.SavedStateHandle
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.usecase.GetCurrentUserIdUseCase
import com.mymovie.log.domain.usecase.GetHolidaysByMonthUseCase
import com.mymovie.log.domain.usecase.GetMovieDetailUseCase
import com.mymovie.log.domain.usecase.GetRecordByTmdbIdUseCase
import com.mymovie.log.domain.usecase.GetRecordsByDateUseCase
import com.mymovie.log.domain.usecase.GetSignedPhotoUrlsUseCase
import com.mymovie.log.domain.usecase.GetWatchedDatesByMonthUseCase
import com.mymovie.log.domain.usecase.SearchMoviesUseCase
import com.mymovie.log.domain.usecase.UploadPhotosUseCase
import com.mymovie.log.domain.usecase.UpsertRecordUseCase
import com.mymovie.log.presentation.calendar.CalendarViewModel
import com.mymovie.log.presentation.detail.DetailUiState
import com.mymovie.log.presentation.detail.MovieDetailViewModel
import com.mymovie.log.presentation.search.SearchViewModel
import com.mymovie.log.presentation.ui.RecordDraft
import com.mymovie.log.testing.FakeAuthRepository
import com.mymovie.log.testing.FakeHolidayRepository
import com.mymovie.log.testing.FakeMovieRecordRepository
import com.mymovie.log.testing.FakeMovieRepository
import com.mymovie.log.testing.FakePhotoRepository
import com.mymovie.log.testing.recreated
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

/**
 * Fold/unfold, rotation, resizing and multi-window changes recreate the Activity. ViewModels
 * survive that on their own; these tests cover the harder case where only SavedStateHandle
 * survives (process recreation), by building a new ViewModel from the old handle's contents.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class StatePreservationTest {

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val recordRepository = FakeMovieRecordRepository()

    private fun movieDetailViewModel(handle: SavedStateHandle) = MovieDetailViewModel(
        getMovieDetailUseCase = GetMovieDetailUseCase(FakeMovieRepository()),
        upsertRecordUseCase = UpsertRecordUseCase(recordRepository),
        uploadPhotosUseCase = UploadPhotosUseCase(FakePhotoRepository()),
        getSignedPhotoUrlsUseCase = GetSignedPhotoUrlsUseCase(FakePhotoRepository()),
        getCurrentUserIdUseCase = GetCurrentUserIdUseCase(FakeAuthRepository()),
        getRecordByTmdbIdUseCase = GetRecordByTmdbIdUseCase(recordRepository),
        savedStateHandle = handle
    )

    @Test
    fun writeLogDraft_survivesRecreation() {
        val handle = SavedStateHandle(mapOf("movieId" to 1))
        val first = movieDetailViewModel(handle)
        first.onRecordClick()
        val draft = RecordDraft(
            status = WatchStatus.WATCHED,
            rating = 4.5f,
            watchedAt = LocalDate.of(2026, 9, 1),
            review = "펼쳤다 접어도 남아 있어야 하는 리뷰"
        )
        first.onDraftChange(draft)

        val restored = movieDetailViewModel(handle.recreated())

        assertTrue(restored.showBottomSheet.value)
        assertEquals(draft, restored.recordDraft.value)
        assertTrue(restored.uiState.value is DetailUiState.Success)
    }

    @Test
    fun keptExistingPhotos_surviveRecreationAndSignedUrlsAreReloaded() {
        recordRepository.records.value = listOf(
            MovieRecord(
                id = "r1", tmdbId = 1, title = "영화 1", status = WatchStatus.WATCHED,
                photoUrls = listOf("user/1/a.jpg", "user/1/b.jpg"),
                photoSourceUris = listOf("content://media/1", "content://media/2")
            )
        )
        val handle = SavedStateHandle(mapOf("movieId" to 1))
        val first = movieDetailViewModel(handle)
        // existingRecord is WhileSubscribed; collect it the way the screen does
        val collector = CoroutineScope(Dispatchers.Main).launch { first.existingRecord.collect {} }
        first.onRecordClick()
        first.removeExistingPhoto("https://signed/user/1/a.jpg")
        collector.cancel()

        val restored = movieDetailViewModel(handle.recreated())

        assertEquals(listOf("content://media/2"), restored.existingPhotoSourceUris.value)
        assertEquals(listOf("https://signed/user/1/b.jpg"), restored.existingPhotoSignedUrls.value)
    }

    @Test
    fun dismissingTheEditor_clearsSavedDraft() {
        val handle = SavedStateHandle(mapOf("movieId" to 1))
        val first = movieDetailViewModel(handle)
        first.onRecordClick()
        first.onDraftChange(RecordDraft(review = "임시"))
        first.onDismissSheet()

        val restored = movieDetailViewModel(handle.recreated())

        assertFalse(restored.showBottomSheet.value)
        assertEquals(RecordDraft(), restored.recordDraft.value)
    }

    @Test
    fun calendarSelection_survivesRecreation() {
        fun calendarViewModel(handle: SavedStateHandle) = CalendarViewModel(
            getWatchedDatesByMonthUseCase = GetWatchedDatesByMonthUseCase(recordRepository),
            getRecordsByDateUseCase = GetRecordsByDateUseCase(recordRepository),
            getHolidaysByMonthUseCase = GetHolidaysByMonthUseCase(FakeHolidayRepository()),
            upsertRecordUseCase = UpsertRecordUseCase(recordRepository),
            savedStateHandle = handle
        )
        val handle = SavedStateHandle()
        val first = calendarViewModel(handle)
        first.onMonthChange(YearMonth.of(2026, 8))
        first.onDateSelected(LocalDate.of(2026, 8, 23))

        val restored = calendarViewModel(handle.recreated())

        assertEquals(YearMonth.of(2026, 8), restored.currentMonth.value)
        assertEquals(LocalDate.of(2026, 8, 23), restored.selectedDate.value)
    }

    @Test
    fun calendarEditDraft_isInitializedFromRecordAndKeptWhileEditing() {
        val handle = SavedStateHandle()
        val viewModel = CalendarViewModel(
            GetWatchedDatesByMonthUseCase(recordRepository),
            GetRecordsByDateUseCase(recordRepository),
            GetHolidaysByMonthUseCase(FakeHolidayRepository()),
            UpsertRecordUseCase(recordRepository),
            handle
        )
        val record = MovieRecord(id = "r1", tmdbId = 1, title = "영화", status = WatchStatus.WATCHED, rating = 3f, review = "원래 리뷰")
        viewModel.selectRecord(record)
        assertEquals(RecordDraft.from(record), viewModel.editDraft.value)

        viewModel.onEditDraftChange(viewModel.editDraft.value.copy(review = "고치는 중"))
        assertEquals("고치는 중", viewModel.editDraft.value.review)
    }

    @Test
    fun searchQuery_survivesRecreation() {
        val handle = SavedStateHandle()
        val first = SearchViewModel(SearchMoviesUseCase(FakeMovieRepository()), handle)
        first.onQueryChange("인터스텔라")

        val restored = SearchViewModel(SearchMoviesUseCase(FakeMovieRepository()), handle.recreated())

        assertEquals("인터스텔라", restored.query.value)
    }
}
