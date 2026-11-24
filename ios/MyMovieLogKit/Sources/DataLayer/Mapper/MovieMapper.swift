import Domain

extension MovieDto {
    func toDomain() -> Movie {
        Movie(
            id: id,
            title: title,
            originalTitle: originalTitle,
            overview: overview,
            posterPath: posterPath,
            backdropPath: backdropPath,
            releaseDate: releaseDate,
            voteAverage: voteAverage,
            genreIds: genreIds
        )
    }
}

extension MovieDetailDto {
    func toDomain() -> Movie {
        Movie(
            id: id,
            title: title,
            originalTitle: originalTitle,
            overview: overview,
            posterPath: posterPath,
            backdropPath: backdropPath,
            releaseDate: releaseDate,
            voteAverage: voteAverage,
            genreIds: genres.map(\.id),
            genres: genres.map(\.name)
        )
    }
}
