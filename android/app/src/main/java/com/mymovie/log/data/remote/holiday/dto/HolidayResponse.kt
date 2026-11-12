package com.mymovie.log.data.remote.holiday.dto

import com.google.gson.JsonElement

data class HolidayResponse(val response: HolidayResponseWrapper)

data class HolidayResponseWrapper(val header: HolidayHeader, val body: HolidayBody)

data class HolidayHeader(val resultCode: String, val resultMsg: String)

// items: API가 totalCount=0 이면 빈 문자열(""), 1건 이상이면 객체({item: ...})로 반환
data class HolidayBody(val items: JsonElement?, val totalCount: Int)

// item: 1건이면 단일 객체, 2건 이상이면 배열 — HolidayRepositoryImpl에서 분기 처리
data class HolidayItem(val dateName: String, val locdate: Long, val isHoliday: String)
