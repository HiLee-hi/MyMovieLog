package com.mymovie.log.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.mymovie.log.data.local.room.AppDatabase
import com.mymovie.log.data.local.room.HolidayDao
import com.mymovie.log.data.local.room.MovieRecordDao
import com.mymovie.log.util.AppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE movie_records ADD COLUMN photo_urls TEXT NOT NULL DEFAULT '[]'")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE movie_records ADD COLUMN photo_source_uris TEXT NOT NULL DEFAULT '[]'")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        AppLogger.i("APP_INIT", "Room database init: mymovie_db")
        return Room.databaseBuilder(context, AppDatabase::class.java, "mymovie_db")
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideMovieRecordDao(db: AppDatabase): MovieRecordDao = db.movieRecordDao()

    @Provides
    @Singleton
    fun provideHolidayDao(db: AppDatabase): HolidayDao = db.holidayDao()
}
