package com.mymovie.log.domain.repository

import com.mymovie.log.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun searchMovies(query: String): Flow<List<Movie>>
    suspend fun getMovieDetail(movieId: Int): Movie
}
