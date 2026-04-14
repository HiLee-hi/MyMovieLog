package com.mymovie.log.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.domain.model.Movie
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.repository.AuthRepository
import com.mymovie.log.domain.usecase.SearchMoviesUseCase
import com.mymovie.log.domain.usecase.UpsertRecordUseCase
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val movies: List<Movie>) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}

sealed class AddRecordState {
    object Idle : AddRecordState()
    object Saving : AddRecordState()
    object Success : AddRecordState()
    data class Error(val message: String) : AddRecordState()
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMoviesUseCase: SearchMoviesUseCase,
    private val upsertRecordUseCase: UpsertRecordUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val uiState: StateFlow<SearchUiState> = _query
        .debounce(400)
        .flatMapLatest { query ->
            if (query.isBlank()) {
                AppLogger.d("VM_SEARCH", "Query cleared → Idle")
                kotlinx.coroutines.flow.flowOf(SearchUiState.Idle)
            } else {
                AppLogger.d("VM_SEARCH", "Searching: queryLength=${query.length}")
                searchMoviesUseCase(query)
                    .map<List<Movie>, SearchUiState> { movies ->
                        AppLogger.d("VM_SEARCH", "Search result: count=${movies.size}")
                        if (movies.isEmpty()) SearchUiState.Success(emptyList())
                        else SearchUiState.Success(movies)
                    }
                    .catch { e ->
                        AppLogger.e("VM_SEARCH", "Search failed: ${e.message}", e)
                        emit(SearchUiState.Error(e.message ?: "검색 실패"))
                    }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SearchUiState.Idle)

    private val _selectedMovie = MutableStateFlow<Movie?>(null)
    val selectedMovie: StateFlow<Movie?> = _selectedMovie.asStateFlow()

    private val _addRecordState = MutableStateFlow<AddRecordState>(AddRecordState.Idle)
    val addRecordState: StateFlow<AddRecordState> = _addRecordState.asStateFlow()

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
    }

    fun onMovieClick(movie: Movie) {
        AppLogger.d("VM_SEARCH", "Movie selected: ${movie.title}")
        _selectedMovie.value = movie
    }

    fun onDismissSheet() {
        _selectedMovie.value = null
        _addRecordState.value = AddRecordState.Idle
    }

    fun resetAddRecordState() {
        _addRecordState.value = AddRecordState.Idle
    }

    fun saveRecord(
        movie: Movie,
        status: WatchStatus,
        rating: Float?,
        watchedAt: LocalDate?,
        review: String?,
        memo: String?
    ) {
        viewModelScope.launch {
            _addRecordState.value = AddRecordState.Saving
            try {
                val userId = authRepository.currentUser.first()?.id ?: ""
                val record = MovieRecord(
                    userId = userId,
                    tmdbId = movie.id,
                    title = movie.title,
                    originalTitle = movie.originalTitle,
                    posterPath = movie.posterPath,
                    genreIds = movie.genreIds,
                    status = status,
                    rating = rating,
                    review = review?.takeIf { it.isNotBlank() },
                    memo = memo?.takeIf { it.isNotBlank() },
                    watchedAt = watchedAt
                )
                AppLogger.i("VM_SEARCH", "Saving record: tmdbId=${movie.id}, status=${status.value}, userId=${AppLogger.shortId(userId)}")
                upsertRecordUseCase(record)
                AppLogger.i("VM_SEARCH", "Record saved successfully")
                _addRecordState.value = AddRecordState.Success
            } catch (e: Exception) {
                AppLogger.e("VM_SEARCH", "Save record failed: ${e.message}", e)
                _addRecordState.value = AddRecordState.Error(e.message ?: "저장 실패")
            }
        }
    }
}
