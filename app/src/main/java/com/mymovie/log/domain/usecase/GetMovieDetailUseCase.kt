package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.model.Movie
import com.mymovie.log.domain.repository.MovieRepository
import javax.inject.Inject

class GetMovieDetailUseCase @Inject constructor(
    private val repository: MovieRepository
) {
    suspend operator fun invoke(movieId: Int): Movie =
        repository.getMovieDetail(movieId)
}
