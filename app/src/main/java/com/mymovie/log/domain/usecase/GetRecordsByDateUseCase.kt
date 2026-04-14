package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.repository.MovieRecordRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject

class GetRecordsByDateUseCase @Inject constructor(
    private val repository: MovieRecordRepository
) {
    operator fun invoke(date: LocalDate): Flow<List<MovieRecord>> =
        repository.getRecordsByDate(date)
}
