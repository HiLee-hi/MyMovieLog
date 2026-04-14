package com.mymovie.log.di

import com.mymovie.log.data.repository.AuthRepositoryImpl
import com.mymovie.log.data.repository.MovieRecordRepositoryImpl
import com.mymovie.log.data.repository.MovieRepositoryImpl
import com.mymovie.log.domain.repository.AuthRepository
import com.mymovie.log.domain.repository.MovieRecordRepository
import com.mymovie.log.domain.repository.MovieRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds
    @Singleton
    abstract fun bindMovieRecordRepository(impl: MovieRecordRepositoryImpl): MovieRecordRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository
}
