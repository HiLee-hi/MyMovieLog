package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.repository.MovieRecordRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecordsUseCase @Inject constructor(
    private val repository: MovieRecordRepository
) {
    operator fun invoke(status: WatchStatus? = null): Flow<List<MovieRecord>> =
        repository.getRecords(status)
}
