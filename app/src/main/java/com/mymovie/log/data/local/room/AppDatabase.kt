package com.mymovie.log.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [MovieRecordEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun movieRecordDao(): MovieRecordDao
}
