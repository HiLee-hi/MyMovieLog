package com.mymovie.log.presentation.camera

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

sealed class CameraUiState {
    object Preview : CameraUiState()
    data class Captured(val tempFile: File) : CameraUiState()
    object Saving : CameraUiState()
    data class SavedToGallery(val uri: Uri) : CameraUiState()
    data class Error(val message: String) : CameraUiState()
}

@HiltViewModel
class CameraViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<CameraUiState>(CameraUiState.Preview)
    val uiState = _uiState.asStateFlow()

    fun onPhotoCaptured(tempFile: File) {
        _uiState.value = CameraUiState.Captured(tempFile)
        AppLogger.d("CAMERA_VM", "Photo captured: ${tempFile.name}")
    }

    fun onCaptureError(message: String) {
        AppLogger.e("CAMERA_VM", "Capture failed: $message")
        _uiState.value = CameraUiState.Error(message)
    }

    fun onRetake() {
        val current = _uiState.value
        if (current is CameraUiState.Captured) {
            current.tempFile.delete()
            AppLogger.d("CAMERA_VM", "Temp file deleted: ${current.tempFile.name}")
        }
        _uiState.value = CameraUiState.Preview
    }

    fun saveToGallery(tempFile: File) {
        _uiState.value = CameraUiState.Saving
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val uri = insertToMediaStore(tempFile)
                    ?: throw IOException("MediaStore insert failed")
                tempFile.delete()
                AppLogger.i("CAMERA_VM", "Saved to gallery: $uri")
                _uiState.value = CameraUiState.SavedToGallery(uri)
            }.onFailure { e ->
                AppLogger.e("CAMERA_VM", "Gallery save failed: ${e.message}", e)
                _uiState.value = CameraUiState.Error(e.message ?: "저장 실패")
            }
        }
    }

    private fun insertToMediaStore(sourceFile: File): Uri? {
        val filename = "mymovie_${System.currentTimeMillis()}.jpg"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/MyMovieLog")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val resolver = appContext.contentResolver
        val destUri = resolver.insert(collection, contentValues) ?: return null

        resolver.openOutputStream(destUri)?.use { out ->
            sourceFile.inputStream().use { it.copyTo(out) }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(destUri, contentValues, null, null)
        }

        // AlbumPickerViewModel과 동일한 EXTERNAL_CONTENT_URI 형식으로 정규화
        val id = destUri.lastPathSegment?.toLongOrNull()
        return if (id != null) {
            ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        } else {
            destUri
        }
    }

    override fun onCleared() {
        super.onCleared()
        val state = _uiState.value
        if (state is CameraUiState.Captured) {
            state.tempFile.delete()
        }
    }
}
