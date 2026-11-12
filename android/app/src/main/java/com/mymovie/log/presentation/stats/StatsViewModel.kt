package com.mymovie.log.presentation.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.usecase.GetRecordsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class StatsUiState(
    val totalWatched: Int = 0,
    val wishlistCount: Int = 0,
    val averageRating: String = "-",
    val thisMonthCount: Int = 0,
    val monthlyStats: Map<String, Int> = emptyMap()
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    getRecordsUseCase: GetRecordsUseCase
) : ViewModel() {

    val uiState: StateFlow<StatsUiState> = getRecordsUseCase()
        .map { allRecords ->
            val watched = allRecords.filter { it.status == WatchStatus.WATCHED }
            val wishlist = allRecords.filter { it.status == WatchStatus.WISHLIST }
            val ratings = watched.mapNotNull { it.rating }
            val avgRating = if (ratings.isEmpty()) "-" else String.format("%.1f", ratings.average())

            val thisMonth = YearMonth.now()
            val thisMonthCount = watched.count { record ->
                record.watchedAt?.let { YearMonth.from(it) == thisMonth } ?: false
            }

            // Stats for the last 6 months
            val formatter = DateTimeFormatter.ofPattern("M월")
            val monthlyStats = (5 downTo 0).associate { offset ->
                val month = thisMonth.minusMonths(offset.toLong())
                val label = month.format(formatter)
                val count = watched.count { record ->
                    record.watchedAt?.let { YearMonth.from(it) == month } ?: false
                }
                label to count
            }

            StatsUiState(
                totalWatched = watched.size,
                wishlistCount = wishlist.size,
                averageRating = avgRating,
                thisMonthCount = thisMonthCount,
                monthlyStats = monthlyStats
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())
}
