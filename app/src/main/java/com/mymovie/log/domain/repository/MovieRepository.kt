package com.mymovie.log.domain.repository

import androidx.paging.PagingData
import com.mymovie.log.domain.model.Movie
import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun searchMovies(query: String): Flow<PagingData<Movie>>
    suspend fun getMovieDetail(movieId: Int): Movie
}
