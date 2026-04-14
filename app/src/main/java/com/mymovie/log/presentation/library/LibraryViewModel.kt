package com.mymovie.log.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.usecase.DeleteRecordUseCase
import com.mymovie.log.domain.usecase.GetRecordsUseCase
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    getRecordsUseCase: GetRecordsUseCase,
    private val deleteRecordUseCase: DeleteRecordUseCase
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(WatchStatus.WATCHED)
    val selectedTab: StateFlow<WatchStatus> = _selectedTab.asStateFlow()

    val watchedRecords: StateFlow<List<MovieRecord>> = getRecordsUseCase(WatchStatus.WATCHED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val wishlistRecords: StateFlow<List<MovieRecord>> = getRecordsUseCase(WatchStatus.WISHLIST)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectTab(status: WatchStatus) {
        AppLogger.d("VM_LIBRARY", "Tab selected: ${status.name}")
        _selectedTab.value = status
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
