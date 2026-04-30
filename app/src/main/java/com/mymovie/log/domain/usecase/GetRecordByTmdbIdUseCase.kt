package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.repository.MovieRecordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecordByTmdbIdUseCase @Inject constructor(
    private val repository: MovieRecordRepository
) {
    operator fun invoke(tmdbId: Int): Flow<MovieRecord?> =
        repository.getRecordByTmdbId(tmdbId)
}
