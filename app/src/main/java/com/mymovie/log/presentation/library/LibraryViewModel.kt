package com.mymovie.log.presentation.library

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.usecase.DeleteRecordUseCase
import com.mymovie.log.domain.usecase.GetRecordsUseCase
import com.mymovie.log.domain.usecase.UpsertRecordUseCase
import com.mymovie.log.presentation.ui.AddRecordState
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    getRecordsUseCase: GetRecordsUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase,
    private val upsertRecordUseCase: UpsertRecordUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val initialTab = savedStateHandle.get<String>("tab")
        ?.let { WatchStatus.from(it) } ?: WatchStatus.WATCHED

    private val _selectedTab = MutableStateFlow(initialTab)
    val selectedTab: StateFlow<WatchStatus> = _selectedTab.asStateFlow()

    val watchedRecords: StateFlow<List<MovieRecord>> = getRecordsUseCase(WatchStatus.WATCHED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wishlistRecords: StateFlow<List<MovieRecord>> = getRecordsUseCase(WatchStatus.WISHLIST)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedRecord = MutableStateFlow<MovieRecord?>(null)
    val selectedRecord: StateFlow<MovieRecord?> = _selectedRecord.asStateFlow()

    private val _editRecordState = MutableStateFlow<AddRecordState>(AddRecordState.Idle)
    val editRecordState: StateFlow<AddRecordState> = _editRecordState.asStateFlow()

    fun selectTab(status: WatchStatus) {
        AppLogger.d("VM_LIBRARY", "Tab selected: ${status.name}")
        _selectedTab.value = status
    }

    fun selectRecord(record: MovieRecord) {
        AppLogger.d("VM_LIBRARY", "Record selected: id=${AppLogger.shortId(record.id)}")
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
            AppLogger.i("VM_LIBRARY", "Update record: id=${AppLogger.shortId(record.id)}, status=${status.value}")
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
                    AppLogger.i("VM_LIBRARY", "Update record success: id=${AppLogger.shortId(record.id)}")
                    _editRecordState.value = AddRecordState.Success
                }
                .onFailure {
                    AppLogger.e("VM_LIBRARY", "Update record failed: ${it.message}", it)
                    _editRecordState.value = AddRecordState.Error(it.message ?: "저장 실패")
                }
        }
    }

    fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            AppLogger.i("VM_LIBRARY", "Delete record requested: id=${AppLogger.shortId(recordId)}")
            runCatching { deleteRecordUseCase(recordId) }
                .onSuccess { AppLogger.i("VM_LIBRARY", "Delete record success: id=${AppLogger.shortId(recordId)}") }
                .onFailure { AppLogger.e("VM_LIBRARY", "Delete record failed: id=${AppLogger.shortId(recordId)}, error=${it.message}", it) }
        }
    }
}
