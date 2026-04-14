package com.mymovie.log.data.repository

import com.mymovie.log.data.local.room.MovieRecordDao
import com.mymovie.log.data.mapper.toDomain
import com.mymovie.log.data.mapper.toDto
import com.mymovie.log.data.mapper.toEntity
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.repository.MovieRecordRepository
import com.mymovie.log.util.AppLogger
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

class MovieRecordRepositoryImpl @Inject constructor(
    private val dao: MovieRecordDao,
    private val postgrest: Postgrest
) : MovieRecordRepository {

    companion object {
        private const val TABLE = "movie_records"
    }

    // Read from local cache (offline support)
    override fun getRecords(status: WatchStatus?): Flow<List<MovieRecord>> {
        AppLogger.d("REPO_RECORD", "getRecords from local: status=${status?.name ?: "ALL"}")
        return if (status == null) {
            dao.getAllRecords().map { entities -> entities.map { it.toDomain() } }
        } else {
            dao.getRecordsByStatus(status.value).map { entities -> entities.map { it.toDomain() } }
        }
    }

    override fun getRecordByTmdbId(tmdbId: Int): Flow<MovieRecord?> =
        dao.getRecordByTmdbId(tmdbId).map { it?.toDomain() }

    override fun getRecordsByDate(date: LocalDate): Flow<List<MovieRecord>> =
        dao.getRecordsByDate(date.toString()).map { entities -> entities.map { it.toDomain() } }

    override fun getWatchedDatesByMonth(year: Int, month: Int): Flow<Set<LocalDate>> {
        val yearMonth = String.format("%04d-%02d", year, month)
        return dao.getWatchedDatesByMonth(yearMonth).map { dateStrings ->
            dateStrings.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet()
        }
    }

    // Save to Supabase first, then update local cache
    override suspend fun upsertRecord(record: MovieRecord) {
        AppLogger.i("REPO_RECORD", "Upsert: tmdbId=${record.tmdbId}, title='${record.title}', status=${record.status.name}")
        try {
            val dto = record.toDto()
            val saved = postgrest[TABLE].upsert(dto) {
                onConflict = "user_id,tmdb_id"
                select()
            }.decodeSingle<com.mymovie.log.data.remote.supabase.dto.MovieRecordDto>()
            dao.upsertRecord(saved.toDomain().toEntity())
            AppLogger.i("REPO_RECORD", "Upsert success: id=${saved.id?.let { AppLogger.shortId(it) }}")
        } catch (e: Exception) {
            AppLogger.e("REPO_RECORD", "Upsert failed: tmdbId=${record.tmdbId}, error=${e.message}", e)
            throw e
        }
    }

    override suspend fun deleteRecord(recordId: String) {
        AppLogger.i("REPO_RECORD", "Delete: id=${AppLogger.shortId(recordId)}")
        try {
            postgrest[TABLE].delete { filter { eq("id", recordId) } }
            dao.deleteRecord(recordId)
            AppLogger.i("REPO_RECORD", "Delete success: id=${AppLogger.shortId(recordId)}")
        } catch (e: Exception) {
            AppLogger.e("REPO_RECORD", "Delete failed: id=${AppLogger.shortId(recordId)}, error=${e.message}", e)
            throw e
        }
    }

    override suspend fun clearLocalCache() {
        AppLogger.i("REPO_RECORD", "Clearing local cache")
        try {
            dao.deleteAll()
            AppLogger.i("REPO_RECORD", "Local cache cleared")
        } catch (e: Exception) {
            AppLogger.e("REPO_RECORD", "Clear local cache failed: ${e.message}", e)
            throw e
        }
    }

    // Full sync from Supabase on login or manual refresh
    override suspend fun syncFromRemote() {
        AppLogger.i("REPO_RECORD", "Sync from remote started")
        try {
            val records = postgrest[TABLE]
                .select()
                .decodeList<com.mymovie.log.data.remote.supabase.dto.MovieRecordDto>()
            dao.upsertAll(records.map { it.toDomain().toEntity() })
            AppLogger.i("REPO_RECORD", "Sync from remote success: count=${records.size}")
        } catch (e: Exception) {
            AppLogger.e("REPO_RECORD", "Sync from remote failed: ${e.message}", e)
            throw e
        }
    }
}
