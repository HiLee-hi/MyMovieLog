package com.mymovie.log.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieRecordDao {

    @Query("SELECT * FROM movie_records ORDER BY created_at DESC")
    fun getAllRecords(): Flow<List<MovieRecordEntity>>

    @Query("SELECT * FROM movie_records WHERE status = :status ORDER BY created_at DESC")
    fun getRecordsByStatus(status: String): Flow<List<MovieRecordEntity>>

    @Query("SELECT * FROM movie_records WHERE tmdb_id = :tmdbId LIMIT 1")
    fun getRecordByTmdbId(tmdbId: Int): Flow<MovieRecordEntity?>

    @Query("SELECT * FROM movie_records WHERE watched_at = :date AND status = 'watched'")
    fun getRecordsByDate(date: String): Flow<List<MovieRecordEntity>>

    // Query only watched dates for a specific month (used for calendar dot markers)
    @Query("""
        SELECT DISTINCT watched_at FROM movie_records
        WHERE status = 'watched'
        AND watched_at LIKE :yearMonth || '%'
        AND watched_at IS NOT NULL
    """)
    fun getWatchedDatesByMonth(yearMonth: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: MovieRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(records: List<MovieRecordEntity>)

    @Query("DELETE FROM movie_records WHERE id = :recordId")
    suspend fun deleteRecord(recordId: String)

    @Query("DELETE FROM movie_records")
    suspend fun deleteAll()
}
