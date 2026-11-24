package com.mymovie.log.presentation

import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.usecase.GetRecordsUseCase
import com.mymovie.log.domain.usecase.ObserveCurrentUserUseCase
import com.mymovie.log.domain.usecase.SignInWithEmailUseCase
import com.mymovie.log.domain.usecase.SignInWithGoogleUseCase
import com.mymovie.log.domain.usecase.SignOutUseCase
import com.mymovie.log.domain.usecase.SignUpWithEmailUseCase
import com.mymovie.log.presentation.profile.ProfileRecordCounts
import com.mymovie.log.presentation.profile.ProfileViewModel
import com.mymovie.log.presentation.stats.StatsViewModel
import com.mymovie.log.testing.FakeAuthRepository
import com.mymovie.log.testing.FakeMovieRecordRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

// counts for the profile cards and the stats "vs last month" line
@OptIn(ExperimentalCoroutinesApi::class)
class RecordCountsTest {

    private val today = LocalDate.now()
    private val lastMonth = today.minusMonths(1)

    private fun record(id: Int, status: WatchStatus, watchedAt: LocalDate? = null) =
        MovieRecord(id = "r$id", tmdbId = id, title = "영화 $id", status = status, watchedAt = watchedAt)

    private val records = listOf(
        record(1, WatchStatus.WATCHED, today),
        record(2, WatchStatus.WATCHED, lastMonth),
        record(3, WatchStatus.WATCHED, lastMonth),
        record(4, WatchStatus.WATCHED, lastMonth),
        record(5, WatchStatus.WISHLIST),
        record(6, WatchStatus.WISHLIST),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun stats_reportsThisAndLastMonth() = runTest {
        val viewModel = StatsViewModel(GetRecordsUseCase(FakeMovieRecordRepository(records)))

        val state = viewModel.uiState.first { it.totalWatched > 0 }

        assertEquals(1, state.thisMonthCount)
        assertEquals(3, state.lastMonthCount)
    }

    @Test
    fun profile_countsWatchedAndWishlist() = runTest {
        val auth = FakeAuthRepository(loggedIn = true)
        val viewModel = ProfileViewModel(
            ObserveCurrentUserUseCase(auth),
            GetRecordsUseCase(FakeMovieRecordRepository(records)),
            SignInWithEmailUseCase(auth),
            SignUpWithEmailUseCase(auth),
            SignInWithGoogleUseCase(auth),
            SignOutUseCase(auth),
        )

        val counts = viewModel.recordCounts.first { it != ProfileRecordCounts() }

        assertEquals(ProfileRecordCounts(watched = 4, wishlist = 2), counts)
    }
}
