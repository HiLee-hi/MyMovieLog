package com.mymovie.log.data.remote.tmdb

import com.mymovie.log.data.remote.tmdb.dto.MovieDetailDto
import com.mymovie.log.data.remote.tmdb.dto.SearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String = "ko-KR",
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false
    ): SearchResponseDto

    @GET("movie/{movie_id}")
    suspend fun getMovieDetail(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "ko-KR"
    ): MovieDetailDto
}
