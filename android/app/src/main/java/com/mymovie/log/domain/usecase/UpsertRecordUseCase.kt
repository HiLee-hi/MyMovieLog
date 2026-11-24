package com.mymovie.log.domain.usecase

import android.net.Uri
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
    // upload newPhotoUris -> save record -> delete removed photos
    // record.photoUrls = photos to keep, uploaded paths appended
    // save fails -> roll back new uploads, removed photo delete fails -> orphan, save still ok
    suspend operator fun invoke(record: MovieRecord, newPhotoUris: List<Uri> = emptyList()) {
        val previousPhotos = repository.getRecordByTmdbId(record.tmdbId).first()?.photoUrls.orEmpty()

        val uploaded = if (newPhotoUris.isEmpty()) emptyList() else {
            AppLogger.i("UC_UPSERT", "Uploading ${newPhotoUris.size} photos")
            photoRepository.uploadPhotos(record.userId, record.tmdbId, newPhotoUris)
        }
        val saved = record.copy(photoUrls = record.photoUrls + uploaded)

        try {
            repository.upsertRecord(saved)
        } catch (e: Exception) {
            if (uploaded.isNotEmpty()) {
                runCatching { photoRepository.deletePhotos(uploaded) }
                    .onFailure { AppLogger.e("UC_UPSERT", "Rollback of uploaded photos failed: count=${uploaded.size}", it) }
            }
            throw e
        }

        val removedPhotos = previousPhotos - saved.photoUrls.toSet()
        if (removedPhotos.isNotEmpty()) {
            runCatching { photoRepository.deletePhotos(removedPhotos) }
                .onFailure { AppLogger.e("UC_UPSERT", "Removed photo cleanup failed: count=${removedPhotos.size}, error=${it.message}", it) }
        }
    }
}
