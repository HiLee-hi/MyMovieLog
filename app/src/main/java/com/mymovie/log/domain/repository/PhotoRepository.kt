package com.mymovie.log.domain.repository

import android.net.Uri

interface PhotoRepository {
    suspend fun uploadPhotos(userId: String, tmdbId: Int, uris: List<Uri>): List<String>
    suspend fun getSignedUrls(paths: List<String>): List<String>
    suspend fun deletePhotos(paths: List<String>)
}
