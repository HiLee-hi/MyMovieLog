package com.mymovie.log.data.mapper

import com.mymovie.log.data.local.room.MovieRecordEntity
import com.mymovie.log.data.remote.supabase.dto.MovieRecordDto
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate

private val json = Json { ignoreUnknownKeys = true }

// DTO (Supabase) -> domain
fun MovieRecordDto.toDomain() = MovieRecord(
    id = id,
    userId = userId,
    tmdbId = tmdbId,
    title = title,
    originalTitle = originalTitle,
    posterPath = posterPath,
    genreIds = genreIds,
    status = WatchStatus.from(status),
    rating = rating,
    review = review,
    memo = memo,
    watchedAt = watchedAt?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    photoUrls = photoUrls,
    createdAt = createdAt
)

// domain -> DTO for upsert, id assigned by server
fun MovieRecord.toDto() = MovieRecordDto(
    id = id,
    userId = userId,
    tmdbId = tmdbId,
    title = title,
    originalTitle = originalTitle,
    posterPath = posterPath,
    genreIds = genreIds,
    status = status.value,
    rating = rating,
    review = review,
    memo = memo,
    watchedAt = watchedAt?.toString(),
    photoUrls = photoUrls
)

// entity (Room) -> domain
fun MovieRecordEntity.toDomain() = MovieRecord(
    id = id,
    userId = userId,
    tmdbId = tmdbId,
    title = title,
    originalTitle = originalTitle,
    posterPath = posterPath,
    genreIds = genreIds.split(",").mapNotNull { it.trim().toIntOrNull() },
    status = WatchStatus.from(status),
    rating = rating,
    review = review,
    memo = memo,
    watchedAt = watchedAt?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    photoUrls = runCatching { json.decodeFromString<List<String>>(photoUrls) }.getOrDefault(emptyList()),
    photoSourceUris = runCatching { json.decodeFromString<List<String>>(photoSourceUris) }.getOrDefault(emptyList()),
    createdAt = createdAt
)

// domain -> entity (Room cache)
fun MovieRecord.toEntity() = MovieRecordEntity(
    id = id,
    userId = userId,
    tmdbId = tmdbId,
    title = title,
    originalTitle = originalTitle,
    posterPath = posterPath,
    genreIds = genreIds.joinToString(","),
    status = status.value,
    rating = rating,
    review = review,
    memo = memo,
    watchedAt = watchedAt?.toString(),
    photoUrls = json.encodeToString(photoUrls),
    photoSourceUris = json.encodeToString(photoSourceUris),
    createdAt = createdAt
)
