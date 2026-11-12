package com.mymovie.log.domain.usecase

import androidx.paging.PagingData
import com.mymovie.log.domain.model.Movie
import com.mymovie.log.domain.repository.MovieRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SearchMoviesUseCase @Inject constructor(
    private val movieRepository: MovieRepository
) {
    operator fun invoke(query: String): Flow<PagingData<Movie>> =
        movieRepository.searchMovies(query)
}
