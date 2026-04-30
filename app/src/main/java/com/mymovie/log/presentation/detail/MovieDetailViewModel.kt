package com.mymovie.log.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.domain.model.Movie
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.repository.AuthRepository
import com.mymovie.log.domain.repository.MovieRepository
import com.mymovie.log.domain.usecase.GetRecordByTmdbIdUseCase
import com.mymovie.log.domain.usecase.UpsertRecordUseCase
import com.mymovie.log.presentation.ui.AddRecordState
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

sealed interface DetailUiState {
    object Loading : DetailUiState
    data class Success(val movie: Movie) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val movieRepository: MovieRepository,
    private val upsertRecordUseCase: UpsertRecordUseCase,
    private val authRepository: AuthRepository,
    private val getRecordByTmdbIdUseCase: GetRecordByTmdbIdUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val movieId: Int = checkNotNull(savedStateHandle["movieId"])

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _showBottomSheet = MutableStateFlow(false)
    val showBottomSheet: StateFlow<Boolean> = _showBottomSheet.asStateFlow()

    private val _addRecordState = MutableStateFlow<AddRecordState>(AddRecordState.Idle)
    val addRecordState: StateFlow<AddRecordState> = _addRecordState.asStateFlow()

    val existingRecord: StateFlow<MovieRecord?> = getRecordByTmdbIdUseCase(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadDetail()
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            AppLogger.d("VM_DETAIL", "Loading movie detail: movieId=$movieId")
            try {
                val movie = movieRepository.getMovieDetail(movieId)
                AppLogger.d("VM_DETAIL", "Movie detail loaded: title='${movie.title}'")
                _uiState.value = DetailUiState.Success(movie)
            } catch (e: Exception) {
                AppLogger.e("VM_DETAIL", "Failed to load movie detail: movieId=$movieId, error=${e.message}", e)
                _uiState.value = DetailUiState.Error(e.message ?: "불러오기 실패")
            }
        }
    }

    fun onRecordClick() {
        _showBottomSheet.value = true
    }

    fun onDismissSheet() {
        _showBottomSheet.value = false
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
                    id = existingRecord.value?.id ?: "",
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
                AppLogger.i("VM_DETAIL", "Saving record: tmdbId=${movie.id}, status=${status.value}, userId=${AppLogger.shortId(userId)}")
                upsertRecordUseCase(record)
                AppLogger.i("VM_DETAIL", "Record saved successfully")
                _addRecordState.value = AddRecordState.Success
            } catch (e: Exception) {
                AppLogger.e("VM_DETAIL", "Save record failed: ${e.message}", e)
                _addRecordState.value = AddRecordState.Error(e.message ?: "저장 실패")
            }
        }
    }
}
