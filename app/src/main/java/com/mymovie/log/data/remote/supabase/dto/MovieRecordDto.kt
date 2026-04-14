package com.mymovie.log.data.remote.supabase.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieRecordDto(
    @SerialName("id") val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("tmdb_id") val tmdbId: Int,
    @SerialName("title") val title: String,
    @SerialName("original_title") val originalTitle: String = "",
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("genre_ids") val genreIds: List<Int> = emptyList(),
    @SerialName("status") val status: String,
    @SerialName("rating") val rating: Float? = null,
    @SerialName("review") val review: String? = null,
    @SerialName("memo") val memo: String? = null,
    @SerialName("watched_at") val watchedAt: String? = null, // "YYYY-MM-DD"
    @SerialName("created_at") val createdAt: String = ""
)
