package com.mymovie.log.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.usecase.GetRecordsUseCase
import com.mymovie.log.domain.usecase.UpsertRecordUseCase
import com.mymovie.log.presentation.ui.AddRecordState
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val recentWatched: List<MovieRecord> = emptyList(),
    val wishlistPreview: List<MovieRecord> = emptyList(),
    val totalWatched: Int = 0,
    val thisMonthCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    getRecordsUseCase: GetRecordsUseCase,
    private val upsertRecordUseCase: UpsertRecordUseCase
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = getRecordsUseCase()
        .map { allRecords ->
            val watched = allRecords.filter { it.status == WatchStatus.WATCHED }
            val wishlist = allRecords.filter { it.status == WatchStatus.WISHLIST }
            val thisMonth = java.time.YearMonth.now()
            val thisMonthCount = watched.count { record ->
                record.watchedAt?.let {
                    java.time.YearMonth.from(it) == thisMonth
                } ?: false
            }
            HomeUiState(
                recentWatched = watched.take(5),
                wishlistPreview = wishlist.take(5),
                totalWatched = watched.size,
                thisMonthCount = thisMonthCount
            )
        }
        .onEach { state ->
            AppLogger.d("VM_HOME", "UiState updated: totalWatched=${state.totalWatched}, thisMonth=${state.thisMonthCount}, wishlist=${state.wishlistPreview.size}")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private val _selectedRecord = MutableStateFlow<MovieRecord?>(null)
    val selectedRecord: StateFlow<MovieRecord?> = _selectedRecord.asStateFlow()

    private val _editRecordState = MutableStateFlow<AddRecordState>(AddRecordState.Idle)
    val editRecordState: StateFlow<AddRecordState> = _editRecordState.asStateFlow()

    fun selectRecord(record: MovieRecord) {
        AppLogger.d("VM_HOME", "Record selected: id=${AppLogger.shortId(record.id)}")
        _selectedRecord.value = record
        _editRecordState.value = AddRecordState.Idle
    }

    fun clearSelectedRecord() {
        _selectedRecord.value = null
        _editRecordState.value = AddRecordState.Idle
    }

    fun updateRecord(
        status: WatchStatus,
        rating: Float?,
        watchedAt: LocalDate?,
        review: String?,
        memo: String?
    ) {
        val record = _selectedRecord.value ?: return
        viewModelScope.launch {
            _editRecordState.value = AddRecordState.Saving
            AppLogger.i("VM_HOME", "Update record: id=${AppLogger.shortId(record.id)}, status=${status.value}")
            runCatching {
                upsertRecordUseCase(
                    record.copy(
                        status = status,
                        rating = rating,
                        watchedAt = watchedAt,
                        review = review?.takeIf { it.isNotBlank() },
                        memo = memo?.takeIf { it.isNotBlank() }
                    )
                )
            }
                .onSuccess {
                    AppLogger.i("VM_HOME", "Update record success: id=${AppLogger.shortId(record.id)}")
                    _editRecordState.value = AddRecordState.Success
                }
                .onFailure {
                    AppLogger.e("VM_HOME", "Update record failed: ${it.message}", it)
                    _editRecordState.value = AddRecordState.Error(it.message ?: "저장 실패")
                }
        }
    }
}
