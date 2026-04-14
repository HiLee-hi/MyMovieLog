package com.mymovie.log.domain.model

import java.time.LocalDate

data class MovieRecord(
    val id: String = "",
    val userId: String = "",
    val tmdbId: Int,
    val title: String,
    val originalTitle: String = "",
    val posterPath: String? = null,
    val genreIds: List<Int> = emptyList(),
    val status: WatchStatus,
    val rating: Float? = null,         // 0.5 ~ 5.0
    val review: String? = null,
    val memo: String? = null,
    val watchedAt: LocalDate? = null,  // date the movie was watched
    val createdAt: String = ""
) {
    val posterUrl: String?
        get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
}

enum class WatchStatus(val value: String) {
    WATCHED("watched"),
    WISHLIST("wishlist");

    companion object {
        fun from(value: String): WatchStatus =
            entries.firstOrNull { it.value == value } ?: WISHLIST
    }
}
