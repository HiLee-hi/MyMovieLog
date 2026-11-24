package com.mymovie.log.domain.repository

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HolidayRepository {
    fun getHolidayDates(year: Int, month: Int): Flow<Set<LocalDate>>
}
