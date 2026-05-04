package com.mymovie.log.data.remote.holiday

import com.mymovie.log.data.remote.holiday.dto.HolidayResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface HolidayApiService {

    @GET("getRestDeInfo")
    suspend fun getHolidays(
        @Query(value = "ServiceKey") serviceKey: String,
        @Query("solYear") year: Int,
        @Query("solMonth") month: String,
        @Query("_type") type: String = "json",
        @Query("numOfRows") numOfRows: Int = 100
    ): HolidayResponse
}
