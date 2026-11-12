package com.mymovie.log.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HolidayDao {

    @Query("SELECT * FROM holidays WHERE year = :year AND month = :month")
    suspend fun getHolidays(year: Int, month: Int): List<HolidayEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHolidays(holidays: List<HolidayEntity>)

    @Query("SELECT COUNT(*) FROM holiday_fetch WHERE year = :year AND month = :month")
    suspend fun isFetched(year: Int, month: Int): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markFetched(entity: HolidayFetchEntity)
}
