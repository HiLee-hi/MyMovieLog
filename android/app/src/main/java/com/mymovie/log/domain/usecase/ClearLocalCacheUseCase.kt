package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.repository.MovieRecordRepository
import javax.inject.Inject

class ClearLocalCacheUseCase @Inject constructor(
    private val repository: MovieRecordRepository
) {
    suspend operator fun invoke() =
        repository.clearLocalCache()
}
