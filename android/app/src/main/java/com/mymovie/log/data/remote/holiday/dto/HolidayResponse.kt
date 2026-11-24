package com.mymovie.log.data.remote.holiday.dto

import com.google.gson.JsonElement

data class HolidayResponse(val response: HolidayResponseWrapper)

data class HolidayResponseWrapper(val header: HolidayHeader, val body: HolidayBody)

data class HolidayHeader(val resultCode: String, val resultMsg: String)

// items: "" when totalCount=0, otherwise {item: ...}
data class HolidayBody(val items: JsonElement?, val totalCount: Int)

// item: object for 1, array for 2+, handled in HolidayRepositoryImpl
data class HolidayItem(val dateName: String, val locdate: Long, val isHoliday: String)
