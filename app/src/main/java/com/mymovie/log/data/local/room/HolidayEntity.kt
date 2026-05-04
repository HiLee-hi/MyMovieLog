package com.mymovie.log.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey val locdate: String, // "20250101"
    val dateName: String,
    val year: Int,
    val month: Int
)

// 해당 (year, month)를 API에서 한 번 이상 조회했음을 표시 — 공휴일 0건 달도 재조회 방지
@Entity(tableName = "holiday_fetch", primaryKeys = ["year", "month"])
data class HolidayFetchEntity(val year: Int, val month: Int)
