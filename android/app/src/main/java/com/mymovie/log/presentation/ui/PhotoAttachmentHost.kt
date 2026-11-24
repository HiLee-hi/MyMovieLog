package com.mymovie.log.presentation.ui

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

// ViewModels with a photo-capable form, lets navigation deliver camera / album results to any of them
interface PhotoAttachmentHost {
    val attachedUris: StateFlow<List<Uri>>
    val existingPhotoSourceUris: StateFlow<List<String>>
    val existingPhotoSignedUrls: StateFlow<List<String>>
    fun addPhoto(uri: Uri)
    fun setPhotos(uris: List<Uri>)
    fun removeExistingPhoto(signedUrl: String)
}
