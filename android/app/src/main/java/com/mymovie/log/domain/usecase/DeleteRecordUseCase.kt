package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.repository.MovieRecordRepository
import com.mymovie.log.domain.repository.PhotoRepository
import com.mymovie.log.util.AppLogger
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class DeleteRecordUseCase @Inject constructor(
    private val repository: MovieRecordRepository,
    private val photoRepository: PhotoRepository
) {
    /**
     * Deletes the record, then its photos in Storage. A failed photo delete leaves an orphaned
     * file but the record stays deleted.
     */
    suspend operator fun invoke(recordId: String) {
        val photos = repository.getRecords().first().firstOrNull { it.id == recordId }?.photoUrls.orEmpty()
        repository.deleteRecord(recordId)
        if (photos.isNotEmpty()) {
            runCatching { photoRepository.deletePhotos(photos) }
                .onFailure { AppLogger.e("UC_DELETE", "Photo cleanup failed: count=${photos.size}, error=${it.message}", it) }
        }
    }
}
