package com.mymovie.log.di

import com.mymovie.log.BuildConfig
import com.mymovie.log.data.remote.tmdb.TmdbApi
import com.mymovie.log.util.AppLogger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        AppLogger.i("APP_INIT", "OkHttpClient init: TMDB_API configured=${BuildConfig.TMDB_API_KEY.isNotBlank()}, debug=${BuildConfig.DEBUG}")
        val authInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_API_KEY}")
                .addHeader("accept", "application/json")
                .build()
            chain.proceed(request)
        }
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideTmdbApi(okHttpClient: OkHttpClient): TmdbApi {
        return Retrofit.Builder()
            .baseUrl(TMDB_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }
}
