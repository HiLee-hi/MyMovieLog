package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.repository.MovieRecordRepository
import com.mymovie.log.domain.repository.PhotoRepository
import com.mymovie.log.util.AppLogger
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class UpsertRecordUseCase @Inject constructor(
    private val repository: MovieRecordRepository,
    private val photoRepository: PhotoRepository
) {
    /**
     * Saves the record, then deletes from Storage the photos the edit removed.
     * Photos are deleted only after the record no longer references them; a failed delete
     * leaves an orphaned file but never fails the save.
     */
    suspend operator fun invoke(record: MovieRecord) {
        val previousPhotos = repository.getRecordByTmdbId(record.tmdbId).first()?.photoUrls.orEmpty()
        repository.upsertRecord(record)
        val removedPhotos = previousPhotos - record.photoUrls.toSet()
        if (removedPhotos.isNotEmpty()) {
            runCatching { photoRepository.deletePhotos(removedPhotos) }
                .onFailure { AppLogger.e("UC_UPSERT", "Removed photo cleanup failed: count=${removedPhotos.size}, error=${it.message}", it) }
        }
    }
}
