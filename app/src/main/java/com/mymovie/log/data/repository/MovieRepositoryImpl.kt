package com.mymovie.log.data.repository

import com.mymovie.log.data.mapper.toDomain
import com.mymovie.log.data.remote.tmdb.TmdbApi
import com.mymovie.log.domain.model.Movie
import com.mymovie.log.domain.repository.MovieRepository
import com.mymovie.log.util.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class MovieRepositoryImpl @Inject constructor(
    private val tmdbApi: TmdbApi
) : MovieRepository {

    override fun searchMovies(query: String): Flow<List<Movie>> = flow {
        if (query.isBlank()) {
            emit(emptyList())
            return@flow
        }
        AppLogger.d("REPO_MOVIE", "TMDB search: query='${query.take(30)}'")
        try {
            val response = tmdbApi.searchMovies(query)
            AppLogger.d("REPO_MOVIE", "TMDB search success: count=${response.results.size}")
            emit(response.results.map { it.toDomain() })
        } catch (e: Exception) {
            AppLogger.e("REPO_MOVIE", "TMDB search failed: ${e.message}", e)
            throw e
        }
    }

    override suspend fun getMovieDetail(movieId: Int): Movie {
        AppLogger.d("REPO_MOVIE", "TMDB getMovieDetail: movieId=$movieId")
        return try {
            val result = tmdbApi.getMovieDetail(movieId).toDomain()
            AppLogger.d("REPO_MOVIE", "TMDB getMovieDetail success: title='${result.title}'")
            result
        } catch (e: Exception) {
            AppLogger.e("REPO_MOVIE", "TMDB getMovieDetail failed: movieId=$movieId, error=${e.message}", e)
            throw e
        }
    }
}
