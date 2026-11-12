package com.mymovie.log.presentation.ui

import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModels whose record form can attach photos. Lets the navigation layer deliver camera /
 * album picker results to whichever screen or pane opened them with one shared implementation.
 */
interface PhotoAttachmentHost {
    val attachedUris: StateFlow<List<Uri>>
    val existingPhotoSourceUris: StateFlow<List<String>>
    val existingPhotoSignedUrls: StateFlow<List<String>>
    fun addPhoto(uri: Uri)
    fun setPhotos(uris: List<Uri>)
    fun removeExistingPhoto(signedUrl: String)
}
