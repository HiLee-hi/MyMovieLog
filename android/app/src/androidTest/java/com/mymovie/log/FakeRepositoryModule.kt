package com.mymovie.log

import com.mymovie.log.di.RepositoryModule
import com.mymovie.log.domain.repository.AuthRepository
import com.mymovie.log.domain.repository.HolidayRepository
import com.mymovie.log.domain.repository.MovieRecordRepository
import com.mymovie.log.domain.repository.MovieRepository
import com.mymovie.log.domain.repository.PhotoRepository
import com.mymovie.log.testing.FakeAuthRepository
import com.mymovie.log.testing.FakeHolidayRepository
import com.mymovie.log.testing.FakeMovieRecordRepository
import com.mymovie.log.testing.FakeMovieRepository
import com.mymovie.log.testing.FakePhotoRepository
import com.mymovie.log.testing.TestViewModels
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

/**
 * The real app (MainActivity, AppNavHost, ViewModels, UseCases) with in-memory repositories and a
 * logged-in fake user, so record writing can be exercised without a real account or network.
 */
@Module
@TestInstallIn(components = [SingletonComponent::class], replaces = [RepositoryModule::class])
object FakeRepositoryModule {
    @Provides @Singleton fun movieRepository(): MovieRepository = FakeMovieRepository()
    @Provides @Singleton fun movieRecordRepository(): MovieRecordRepository =
        FakeMovieRecordRepository(TestViewModels.sampleRecords())
    @Provides @Singleton fun authRepository(): AuthRepository = FakeAuthRepository(loggedIn = true)
    @Provides @Singleton fun holidayRepository(): HolidayRepository = FakeHolidayRepository()
    @Provides @Singleton fun photoRepository(): PhotoRepository = FakePhotoRepository()
}
