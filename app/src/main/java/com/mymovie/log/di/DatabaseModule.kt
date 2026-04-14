package com.mymovie.log.di

import android.content.Context
import androidx.room.Room
import com.mymovie.log.data.local.room.AppDatabase
import com.mymovie.log.data.local.room.MovieRecordDao
import com.mymovie.log.util.AppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        AppLogger.i("APP_INIT", "Room database init: mymovie_db")
        return Room.databaseBuilder(context, AppDatabase::class.java, "mymovie_db")
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideMovieRecordDao(db: AppDatabase): MovieRecordDao = db.movieRecordDao()
}
