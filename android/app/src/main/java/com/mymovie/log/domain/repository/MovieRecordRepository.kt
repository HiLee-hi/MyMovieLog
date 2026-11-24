package com.mymovie.log.domain.repository

import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface MovieRecordRepository {
    fun getRecords(status: WatchStatus? = null): Flow<List<MovieRecord>>
    fun getRecordByTmdbId(tmdbId: Int): Flow<MovieRecord?>
    fun getRecordsByDate(date: LocalDate): Flow<List<MovieRecord>>
    fun getWatchedDatesByMonth(year: Int, month: Int): Flow<Set<LocalDate>>
    suspend fun upsertRecord(record: MovieRecord)
    suspend fun deleteRecord(recordId: String)
    suspend fun syncFromRemote()
    suspend fun clearLocalCache()
}
