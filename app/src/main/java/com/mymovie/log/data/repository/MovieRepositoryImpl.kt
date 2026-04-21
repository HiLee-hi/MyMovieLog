package com.mymovie.log.data.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.mymovie.log.data.mapper.toDomain
import com.mymovie.log.data.remote.tmdb.MovieSearchPagingSource

import com.mymovie.log.data.remote.tmdb.TmdbApi
import com.mymovie.log.domain.model.Movie
import com.mymovie.log.domain.repository.MovieRepository
import com.mymovie.log.util.AppLogger
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MovieRepositoryImpl @Inject constructor(
    private val tmdbApi: TmdbApi
) : MovieRepository {

    override fun searchMovies(query: String): Flow<PagingData<Movie>> {
        AppLogger.d("REPO_MOVIE", "TMDB search paging: query='${query.take(30)}'")
        return Pager(
            config = PagingConfig(pageSize = 20, enablePlaceholders = false),
            pagingSourceFactory = { MovieSearchPagingSource(tmdbApi, query) }
        ).flow
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
