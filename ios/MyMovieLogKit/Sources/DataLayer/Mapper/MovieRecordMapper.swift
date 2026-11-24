import Domain

// DTO (Supabase / cache file) -> domain
extension MovieRecordDto {
    func toDomain() -> MovieRecord {
        MovieRecord(
            id: id ?? "",
            userId: userId,
            tmdbId: tmdbId,
            title: title,
            originalTitle: originalTitle,
            posterPath: posterPath,
            genreIds: genreIds,
            status: WatchStatus.from(status),
            rating: rating,
            review: review,
            memo: memo,
            watchedAt: watchedAt.flatMap(LocalDate.init(isoString:)),
            photoUrls: photoUrls,
            createdAt: createdAt ?? ""
        )
    }
}

// domain -> DTO, id / created_at assigned by server
extension MovieRecord {
    func toDto() -> MovieRecordDto {
        MovieRecordDto(
            id: id.isEmpty ? nil : id,
            userId: userId,
            tmdbId: tmdbId,
            title: title,
            originalTitle: originalTitle,
            posterPath: posterPath,
            genreIds: genreIds,
            status: status.rawValue,
            rating: rating,
            review: review,
            memo: memo,
            watchedAt: watchedAt?.isoString,
            photoUrls: photoUrls,
            createdAt: createdAt.isEmpty ? nil : createdAt
        )
    }
}
