package com.mymovie.log.presentation.calendar

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.usecase.GetHolidaysByMonthUseCase
import com.mymovie.log.domain.usecase.GetRecordsByDateUseCase
import com.mymovie.log.domain.usecase.GetWatchedDatesByMonthUseCase
import com.mymovie.log.domain.usecase.UpsertRecordUseCase
import com.mymovie.log.presentation.ui.AddRecordState
import com.mymovie.log.presentation.ui.RecordDraft
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getWatchedDatesByMonthUseCase: GetWatchedDatesByMonthUseCase,
    private val getRecordsByDateUseCase: GetRecordsByDateUseCase,
    private val getHolidaysByMonthUseCase: GetHolidaysByMonthUseCase,
    private val upsertRecordUseCase: UpsertRecordUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Month and date are kept in SavedStateHandle so they survive fold/unfold, rotation,
    // resizing and process recreation.

    // Currently displayed month
    private val _currentMonth = MutableStateFlow(savedStateHandle[KEY_CURRENT_MONTH] ?: YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    // Selected date (BottomSheet on compact windows, records pane on wide windows)
    private val _selectedDate = MutableStateFlow<LocalDate?>(savedStateHandle[KEY_SELECTED_DATE])
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

    val holidayDates: StateFlow<Set<LocalDate>> = _currentMonth
        .flatMapLatest { month ->
            getHolidaysByMonthUseCase(month.year, month.monthValue)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // Set of watched dates in the current month (used for dot markers)
    val watchedDates: StateFlow<Set<LocalDate>> = _currentMonth
        .flatMapLatest { month ->
            getWatchedDatesByMonthUseCase(month.year, month.monthValue)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // List of records for the selected date (BottomSheet content)
    val selectedDateRecords: StateFlow<List<MovieRecord>> = _selectedDate
        .flatMapLatest { date ->
            if (date == null) {
                kotlinx.coroutines.flow.flowOf(emptyList())
            } else {
                getRecordsByDateUseCase(date)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onMonthChange(yearMonth: YearMonth) {
        AppLogger.d("VM_CALENDAR", "Month changed: $yearMonth")
        _currentMonth.value = yearMonth
        savedStateHandle[KEY_CURRENT_MONTH] = yearMonth
    }

    fun onDateSelected(date: LocalDate) {
        AppLogger.d("VM_CALENDAR", "Date selected: $date")
        setSelectedDate(date)
    }

    fun onBottomSheetDismissed() {
        AppLogger.d("VM_CALENDAR", "BottomSheet dismissed")
        setSelectedDate(null)
        clearSelectedRecord()
    }

    private fun setSelectedDate(date: LocalDate?) {
        _selectedDate.value = date
        savedStateHandle[KEY_SELECTED_DATE] = date
    }

    private val _selectedRecord = MutableStateFlow<MovieRecord?>(null)
    val selectedRecord: StateFlow<MovieRecord?> = _selectedRecord.asStateFlow()

    private val _editRecordState = MutableStateFlow<AddRecordState>(AddRecordState.Idle)
    val editRecordState: StateFlow<AddRecordState> = _editRecordState.asStateFlow()

    // Edit form values live here so they survive the switch between the sheet and the pane
    private val _editDraft = MutableStateFlow(RecordDraft())
    val editDraft: StateFlow<RecordDraft> = _editDraft.asStateFlow()

    fun selectRecord(record: MovieRecord) {
        _selectedRecord.value = record
        _editRecordState.value = AddRecordState.Idle
        _editDraft.value = RecordDraft.from(record)
    }

    fun onEditDraftChange(draft: RecordDraft) {
        _editDraft.value = draft
    }

    fun clearSelectedRecord() {
        _selectedRecord.value = null
        _editRecordState.value = AddRecordState.Idle
        _editDraft.value = RecordDraft()
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
                .onSuccess { _editRecordState.value = AddRecordState.Success }
                .onFailure { _editRecordState.value = AddRecordState.Error(it.message ?: "저장 실패") }
        }
    }

    private companion object {
        const val KEY_CURRENT_MONTH = "calendar_current_month"
        const val KEY_SELECTED_DATE = "calendar_selected_date"
    }
}
