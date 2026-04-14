package com.mymovie.log.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.usecase.GetRecordsUseCase
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val recentWatched: List<MovieRecord> = emptyList(),
    val wishlistPreview: List<MovieRecord> = emptyList(),
    val totalWatched: Int = 0,
    val thisMonthCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    getRecordsUseCase: GetRecordsUseCase
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
}
