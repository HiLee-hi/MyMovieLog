package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.repository.MovieRecordRepository
import javax.inject.Inject

class UpsertRecordUseCase @Inject constructor(
    private val repository: MovieRecordRepository
) {
    suspend operator fun invoke(record: MovieRecord) =
        repository.upsertRecord(record)
}
