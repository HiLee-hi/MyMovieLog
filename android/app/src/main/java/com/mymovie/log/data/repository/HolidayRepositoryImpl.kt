package com.mymovie.log.data.repository

import com.google.gson.Gson
import com.mymovie.log.BuildConfig
import com.mymovie.log.data.local.room.HolidayDao
import com.mymovie.log.data.local.room.HolidayEntity
import com.mymovie.log.data.local.room.HolidayFetchEntity
import com.mymovie.log.data.remote.holiday.HolidayApiService
import com.mymovie.log.data.remote.holiday.dto.HolidayBody
import com.mymovie.log.data.remote.holiday.dto.HolidayItem
import com.mymovie.log.domain.repository.HolidayRepository
import com.mymovie.log.util.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class HolidayRepositoryImpl @Inject constructor(
    private val apiService: HolidayApiService,
    private val holidayDao: HolidayDao
) : HolidayRepository {

    private val gson = Gson()
    private val basicIsoDate = DateTimeFormatter.BASIC_ISO_DATE

    override fun getHolidayDates(year: Int, month: Int): Flow<Set<LocalDate>> = flow {
        if (BuildConfig.PUBLIC_DATA_API_KEY.isBlank()) {
            AppLogger.w("HOLIDAY_REPO", "PUBLIC_DATA_API_KEY not set — skipping holiday fetch")
            emit(emptySet())
            return@flow
        }

        if (holidayDao.isFetched(year, month) > 0) {
            val cached = holidayDao.getHolidays(year, month).toLocalDateSet()
            AppLogger.d("HOLIDAY_REPO", "Cache hit $year/$month → ${cached.size} holidays")
            emit(cached)
            return@flow
        }

        try {
            val response = apiService.getHolidays(
                serviceKey = BuildConfig.PUBLIC_DATA_API_KEY,
                year = year,
                month = month.toString().padStart(2, '0')
            )

            val header = response.response.header
            AppLogger.i("HOLIDAY_REPO", "API $year/$month resultCode=${header.resultCode} msg=${header.resultMsg}")

            // API 오류 응답 시 캐시하지 않고 빈 결과만 반환 — 다음 조회 때 재시도
            if (header.resultCode != "00") {
                AppLogger.w("HOLIDAY_REPO", "API error for $year/$month — will retry next load")
                emit(emptySet())
                return@flow
            }

            val items = parseItems(response.response.body)
            AppLogger.d("HOLIDAY_REPO", "Parsed ${items.size} raw items (isHoliday=Y/N) for $year/$month")

            val entities = items
                .filter { it.isHoliday == "Y" }
                .map { item ->
                    HolidayEntity(
                        locdate = item.locdate.toString(),
                        dateName = item.dateName,
                        year = year,
                        month = month
                    )
                }
            holidayDao.insertHolidays(entities)
            holidayDao.markFetched(HolidayFetchEntity(year, month))
            AppLogger.i("HOLIDAY_REPO", "Cached ${entities.size} holidays for $year/$month: ${entities.map { it.dateName }}")
            emit(entities.toLocalDateSet())
        } catch (e: Exception) {
            AppLogger.e("HOLIDAY_REPO", "Failed to fetch holidays for $year/$month: ${e.message}", e)
            emit(emptySet())
        }
    }

    private fun parseItems(body: HolidayBody): List<HolidayItem> {
        if (body.totalCount == 0) return emptyList()
        val items = body.items ?: return emptyList()
        if (!items.isJsonObject) return emptyList()
        val itemEl = items.asJsonObject["item"] ?: return emptyList()
        return when {
            itemEl.isJsonArray -> gson.fromJson(itemEl, Array<HolidayItem>::class.java).toList()
            itemEl.isJsonObject -> listOf(gson.fromJson(itemEl, HolidayItem::class.java))
            else -> emptyList()
        }
    }

    private fun List<HolidayEntity>.toLocalDateSet(): Set<LocalDate> =
        mapNotNull { runCatching { LocalDate.parse(it.locdate, basicIsoDate) }.getOrNull() }.toSet()
}
