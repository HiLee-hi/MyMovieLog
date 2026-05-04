package com.mymovie.log.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MovieRecordEntity::class, HolidayEntity::class, HolidayFetchEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieRecordDao(): MovieRecordDao
    abstract fun holidayDao(): HolidayDao
}
