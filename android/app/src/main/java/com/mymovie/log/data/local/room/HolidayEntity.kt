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

// marks (year, month) as fetched, so months with 0 holidays aren't refetched
@Entity(tableName = "holiday_fetch", primaryKeys = ["year", "month"])
data class HolidayFetchEntity(val year: Int, val month: Int)
