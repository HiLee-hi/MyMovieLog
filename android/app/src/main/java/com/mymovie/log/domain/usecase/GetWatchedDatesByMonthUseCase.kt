package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.repository.MovieRecordRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetWatchedDatesByMonthUseCase @Inject constructor(
    private val repository: MovieRecordRepository
) {
    operator fun invoke(year: Int, month: Int): Flow<Set<LocalDate>> =
        repository.getWatchedDatesByMonth(year, month)
}
