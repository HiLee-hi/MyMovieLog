package com.mymovie.log.domain.usecase

import android.net.Uri
import com.mymovie.log.domain.repository.PhotoRepository
import javax.inject.Inject

class UploadPhotosUseCase @Inject constructor(
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(userId: String, tmdbId: Int, uris: List<Uri>): List<String> =
        photoRepository.uploadPhotos(userId, tmdbId, uris)
}
