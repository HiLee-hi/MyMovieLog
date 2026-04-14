package com.mymovie.log.data.mapper

import com.mymovie.log.data.remote.tmdb.dto.MovieDetailDto
import com.mymovie.log.data.remote.tmdb.dto.MovieDto
import com.mymovie.log.domain.model.Movie

fun MovieDto.toDomain() = Movie(
    id = id,
    title = title,
    originalTitle = originalTitle,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    genreIds = genreIds
)

fun MovieDetailDto.toDomain() = Movie(
    id = id,
    title = title,
    originalTitle = originalTitle,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    releaseDate = releaseDate,
    voteAverage = voteAverage,
    genreIds = genres.map { it.id }
)
