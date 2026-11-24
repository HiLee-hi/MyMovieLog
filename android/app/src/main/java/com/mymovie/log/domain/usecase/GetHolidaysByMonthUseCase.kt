package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.repository.HolidayRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetHolidaysByMonthUseCase @Inject constructor(
    private val repository: HolidayRepository
) {
    operator fun invoke(year: Int, month: Int): Flow<Set<LocalDate>> =
        repository.getHolidayDates(year, month)
}
