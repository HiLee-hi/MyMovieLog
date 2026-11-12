package com.mymovie.log.domain.usecase

import com.mymovie.log.domain.repository.PhotoRepository
import javax.inject.Inject

class GetSignedPhotoUrlsUseCase @Inject constructor(
    private val photoRepository: PhotoRepository
) {
    suspend operator fun invoke(paths: List<String>): List<String> =
        photoRepository.getSignedUrls(paths)
}
