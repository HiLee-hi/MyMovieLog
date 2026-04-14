package com.mymovie.log.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.usecase.GetRecordsByDateUseCase
import com.mymovie.log.domain.usecase.GetWatchedDatesByMonthUseCase
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val getWatchedDatesByMonthUseCase: GetWatchedDatesByMonthUseCase,
    private val getRecordsByDateUseCase: GetRecordsByDateUseCase
) : ViewModel() {

    // Currently displayed month
    private val _currentMonth = MutableStateFlow(YearMonth.now())
    val currentMonth: StateFlow<YearMonth> = _currentMonth.asStateFlow()

    // Selected date (triggers BottomSheet display)
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()

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
    }

    fun onDateSelected(date: LocalDate) {
        AppLogger.d("VM_CALENDAR", "Date selected: $date")
        _selectedDate.value = date
    }

    fun onBottomSheetDismissed() {
        AppLogger.d("VM_CALENDAR", "BottomSheet dismissed")
        _selectedDate.value = null
    }
}
