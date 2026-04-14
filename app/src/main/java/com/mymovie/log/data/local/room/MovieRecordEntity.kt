package com.mymovie.log.data.local.room

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "movie_records")
data class MovieRecordEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "tmdb_id") val tmdbId: Int,
    val title: String,
    @ColumnInfo(name = "original_title") val originalTitle: String,
    @ColumnInfo(name = "poster_path") val posterPath: String?,
    @ColumnInfo(name = "genre_ids") val genreIds: String,    // comma-separated string, e.g. "18,28"
    val status: String,
    val rating: Float?,
    val review: String?,
    val memo: String?,
    @ColumnInfo(name = "watched_at") val watchedAt: String?,  // "YYYY-MM-DD"
    @ColumnInfo(name = "created_at") val createdAt: String
)
