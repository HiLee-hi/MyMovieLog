package com.mymovie.log.presentation.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mymovie.log.presentation.ui.PhotoAttachmentHost
import com.mymovie.log.util.AppLogger

private const val KEY_CAPTURED_PHOTO_URI = "capturedPhotoUri"
private const val KEY_SELECTED_PHOTOS = "selectedPhotos"
internal const val KEY_ALREADY_ATTACHED_URIS = "alreadyAttachedUris"
internal const val KEY_EXISTING_PHOTO_COUNT = "existingPhotoCount"

/**
 * Delivers Camera / AlbumPicker results written to [savedStateHandle] (the back stack entry that
 * opened them) to [host]. Results live in the SavedStateHandle, so they are not lost if the
 * window changes (fold/unfold, rotation) while the camera or picker is open.
 */
@Composable
internal fun PhotoPickerResultEffect(
    savedStateHandle: SavedStateHandle,
    host: PhotoAttachmentHost,
    logLabel: String,
) {
    val capturedPhotoUri by savedStateHandle
        .getStateFlow(KEY_CAPTURED_PHOTO_URI, "")
        .collectAsStateWithLifecycle()
    LaunchedEffect(capturedPhotoUri, host) {
        if (capturedPhotoUri.isNotBlank()) {
            AppLogger.d("NAVIGATION", "$logLabel: Camera result received: $capturedPhotoUri")
            host.addPhoto(Uri.parse(capturedPhotoUri))
            savedStateHandle[KEY_CAPTURED_PHOTO_URI] = ""
        }
    }

    val selectedPhotos by savedStateHandle
        .getStateFlow<List<String>>(KEY_SELECTED_PHOTOS, emptyList())
        .collectAsStateWithLifecycle()
    LaunchedEffect(selectedPhotos, host) {
        if (selectedPhotos.isNotEmpty()) {
            val existingSourceUris = host.existingPhotoSourceUris.value
            val existingSignedUrls = host.existingPhotoSignedUrls.value
            val returnedSet = selectedPhotos.toSet()
            existingSourceUris.forEachIndexed { index, sourceUri ->
                if (sourceUri !in returnedSet && index < existingSignedUrls.size) {
                    host.removeExistingPhoto(existingSignedUrls[index])
                }
            }
            val newUris = selectedPhotos
                .map { Uri.parse(it) }
                .filter { it.toString() !in existingSourceUris.toSet() }
            AppLogger.d("NAVIGATION", "$logLabel: AlbumPicker result: ${selectedPhotos.size} total, ${newUris.size} new")
            host.setPhotos(newUris)
            savedStateHandle[KEY_SELECTED_PHOTOS] = emptyList<String>()
        }
    }
}

/** Opens the album picker with the photos [host] already has, as the original screens did. */
internal fun openAlbumPicker(
    navController: NavController,
    savedStateHandle: SavedStateHandle,
    host: PhotoAttachmentHost,
    alreadyAttached: List<Uri>,
    logLabel: String,
) {
    val existingSourceUris = host.existingPhotoSourceUris.value
    val existingSignedUrls = host.existingPhotoSignedUrls.value
    val combined = alreadyAttached + existingSourceUris.map { Uri.parse(it) }
    val unknownCount = (existingSignedUrls.size - existingSourceUris.size).coerceAtLeast(0)
    AppLogger.d("NAVIGATION", "$logLabel → AlbumPicker: ${combined.size} attached, $unknownCount unknown")
    savedStateHandle[KEY_ALREADY_ATTACHED_URIS] = combined.map { it.toString() }
    savedStateHandle[KEY_EXISTING_PHOTO_COUNT] = unknownCount
    navController.navigate(Screen.AlbumPicker.route)
}

internal fun deliverCapturedPhoto(navController: NavController, uri: Uri) {
    navController.previousBackStackEntry?.savedStateHandle?.set(KEY_CAPTURED_PHOTO_URI, uri.toString())
}

internal fun deliverSelectedPhotos(navController: NavController, uris: List<Uri>) {
    navController.previousBackStackEntry?.savedStateHandle?.set(KEY_SELECTED_PHOTOS, uris.map { it.toString() })
}
