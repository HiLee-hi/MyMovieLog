package com.mymovie.log.testing

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.mymovie.log.domain.model.Movie
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.UserProfile
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.repository.AuthRepository
import com.mymovie.log.domain.repository.HolidayRepository
import com.mymovie.log.domain.repository.MovieRecordRepository
import com.mymovie.log.domain.repository.MovieRepository
import com.mymovie.log.domain.repository.PhotoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import java.time.LocalDate

fun testMovie(id: Int = 1) = Movie(
    id = id,
    title = "영화 $id",
    originalTitle = "Movie $id",
    overview = "줄거리 $id",
    posterPath = null,
    backdropPath = null,
    releaseDate = "2024-01-01",
    voteAverage = 7.5,
    genreIds = emptyList(),
    genres = listOf("드라마")
)

class FakeMovieRepository(private val movies: List<Movie> = listOf(testMovie(1), testMovie(2))) : MovieRepository {
    // Explicit NotLoading states, like a PagingSource that finished loading
    override fun searchMovies(query: String): Flow<PagingData<Movie>> = flowOf(
        PagingData.from(
            movies,
            sourceLoadStates = LoadStates(
                refresh = LoadState.NotLoading(endOfPaginationReached = false),
                prepend = LoadState.NotLoading(endOfPaginationReached = true),
                append = LoadState.NotLoading(endOfPaginationReached = true)
            )
        )
    )
    override suspend fun getMovieDetail(movieId: Int): Movie = movies.firstOrNull { it.id == movieId } ?: testMovie(movieId)
}

class FakeMovieRecordRepository(initial: List<MovieRecord> = emptyList()) : MovieRecordRepository {
    val records = MutableStateFlow(initial)
    override fun getRecords(status: WatchStatus?): Flow<List<MovieRecord>> =
        records.map { list -> if (status == null) list else list.filter { it.status == status } }
    override fun getRecordByTmdbId(tmdbId: Int): Flow<MovieRecord?> =
        records.map { list -> list.firstOrNull { it.tmdbId == tmdbId } }
    override fun getRecordsByDate(date: LocalDate): Flow<List<MovieRecord>> =
        records.map { list -> list.filter { it.watchedAt == date } }
    override fun getWatchedDatesByMonth(year: Int, month: Int): Flow<Set<LocalDate>> =
        records.map { list -> list.mapNotNull { it.watchedAt }.filter { it.year == year && it.monthValue == month }.toSet() }
    override suspend fun upsertRecord(record: MovieRecord) {
        records.value = records.value.filterNot { it.id == record.id } + record
    }
    override suspend fun deleteRecord(recordId: String) {
        records.value = records.value.filterNot { it.id == recordId }
    }
    override suspend fun syncFromRemote() = Unit
    override suspend fun clearLocalCache() = Unit
}

class FakePhotoRepository : PhotoRepository {
    override suspend fun uploadPhotos(userId: String, tmdbId: Int, uris: List<Uri>): List<String> = emptyList()
    override suspend fun getSignedUrls(paths: List<String>): List<String> = paths.map { "https://signed/$it" }
    override suspend fun deletePhotos(paths: List<String>) = Unit
}

class FakeAuthRepository(loggedIn: Boolean = false) : AuthRepository {
    override val currentUser: Flow<UserProfile?> = flowOf(
        if (loggedIn) UserProfile(id = "test-user", email = "tester@example.com", displayName = "테스터", avatarUrl = null) else null
    )
    override suspend fun signInWithEmail(email: String, password: String) = Unit
    override suspend fun signUpWithEmail(email: String, password: String) = Unit
    override suspend fun signInWithGoogle() = Unit
    override suspend fun signOut() = Unit
    override fun isLoggedIn(): Boolean = true
}

class FakeHolidayRepository : HolidayRepository {
    override fun getHolidayDates(year: Int, month: Int): Flow<Set<LocalDate>> = flowOf(emptySet())
}

/** What survives process death: a new SavedStateHandle built from the old one's contents. */
fun SavedStateHandle.recreated(): SavedStateHandle =
    SavedStateHandle(keys().associateWith { get<Any?>(it) })
