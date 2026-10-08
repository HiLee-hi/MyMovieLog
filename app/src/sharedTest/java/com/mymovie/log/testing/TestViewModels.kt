package com.mymovie.log.testing

import androidx.lifecycle.SavedStateHandle
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.usecase.GetCurrentUserIdUseCase
import com.mymovie.log.domain.usecase.GetHolidaysByMonthUseCase
import com.mymovie.log.domain.usecase.GetMovieDetailUseCase
import com.mymovie.log.domain.usecase.GetRecordByTmdbIdUseCase
import com.mymovie.log.domain.usecase.GetRecordsByDateUseCase
import com.mymovie.log.domain.usecase.GetRecordsUseCase
import com.mymovie.log.domain.usecase.GetSignedPhotoUrlsUseCase
import com.mymovie.log.domain.usecase.GetWatchedDatesByMonthUseCase
import com.mymovie.log.domain.usecase.SearchMoviesUseCase
import com.mymovie.log.domain.usecase.UploadPhotosUseCase
import com.mymovie.log.domain.usecase.UpsertRecordUseCase
import com.mymovie.log.presentation.calendar.CalendarViewModel
import com.mymovie.log.presentation.detail.MovieDetailViewModel
import com.mymovie.log.presentation.home.HomeViewModel
import com.mymovie.log.presentation.search.SearchViewModel
import com.mymovie.log.presentation.stats.StatsViewModel
import java.time.LocalDate

/** Real ViewModels wired to in-memory fakes, so tests render the actual screens. */
object TestViewModels {

    val watchedDate: LocalDate = LocalDate.now().withDayOfMonth(10)

    fun sampleRecords(): List<MovieRecord> = (1..6).map { i ->
        MovieRecord(
            id = "r$i",
            tmdbId = i,
            title = "영화 $i",
            status = if (i <= 4) WatchStatus.WATCHED else WatchStatus.WISHLIST,
            rating = if (i <= 4) 3.5f else null,
            review = if (i <= 4) "리뷰 $i" else null,
            watchedAt = if (i <= 4) watchedDate else null
        )
    }

    fun recordRepository() = FakeMovieRecordRepository(sampleRecords())

    fun home(repo: FakeMovieRecordRepository = recordRepository()) = HomeViewModel(
        GetRecordsUseCase(repo),
        UpsertRecordUseCase(repo),
        UploadPhotosUseCase(FakePhotoRepository()),
        GetSignedPhotoUrlsUseCase(FakePhotoRepository()),
        GetCurrentUserIdUseCase(FakeAuthRepository())
    )

    fun stats(repo: FakeMovieRecordRepository = recordRepository()) = StatsViewModel(GetRecordsUseCase(repo))

    fun calendar(
        repo: FakeMovieRecordRepository = recordRepository(),
        handle: SavedStateHandle = SavedStateHandle()
    ) = CalendarViewModel(
        GetWatchedDatesByMonthUseCase(repo),
        GetRecordsByDateUseCase(repo),
        GetHolidaysByMonthUseCase(FakeHolidayRepository()),
        UpsertRecordUseCase(repo),
        handle
    )

    fun movieDetail(
        movieId: Int = 1,
        repo: FakeMovieRecordRepository = recordRepository(),
        handle: SavedStateHandle = SavedStateHandle(mapOf("movieId" to movieId))
    ) = MovieDetailViewModel(
        GetMovieDetailUseCase(FakeMovieRepository()),
        UpsertRecordUseCase(repo),
        UploadPhotosUseCase(FakePhotoRepository()),
        GetSignedPhotoUrlsUseCase(FakePhotoRepository()),
        GetCurrentUserIdUseCase(FakeAuthRepository()),
        GetRecordByTmdbIdUseCase(repo),
        handle
    )

    fun search(handle: SavedStateHandle = SavedStateHandle()) =
        SearchViewModel(SearchMoviesUseCase(FakeMovieRepository()), handle)
}
