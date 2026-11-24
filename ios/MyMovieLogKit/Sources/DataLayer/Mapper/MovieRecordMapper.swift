import Domain

// DTO (Supabase / cache file) → Domain model
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

// Domain model → DTO (id and created_at are assigned by the server)
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
