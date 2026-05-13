package com.mymovie.log.presentation.picker

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class GalleryPhoto(val uri: Uri, val dateAdded: Long)

sealed class AlbumPickerUiState {
    object Loading : AlbumPickerUiState()
    data class Ready(val photos: List<GalleryPhoto>) : AlbumPickerUiState()
    data class Error(val message: String) : AlbumPickerUiState()
}

private const val MAX_TOTAL_PHOTOS = 10

@HiltViewModel
class AlbumPickerViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<AlbumPickerUiState>(AlbumPickerUiState.Loading)
    val uiState = _uiState.asStateFlow()

    private val _selectedUris = MutableStateFlow<List<Uri>>(emptyList())
    val selectedUris = _selectedUris.asStateFlow()

    // 앨범 외부에서 이미 점유된 사진 수 (기존 저장 사진 - content:// URI 없이 Supabase URL만 있는 것)
    private var reservedCount = 0

    fun initialize(alreadyAttached: List<Uri>, existingPhotoCount: Int = 0) {
        reservedCount = existingPhotoCount
        _selectedUris.value = alreadyAttached.toList()
        loadPhotos()
    }

    fun toggleSelection(uri: Uri) {
        val current = _selectedUris.value.toMutableList()
        val uriString = uri.toString()
        val index = current.indexOfFirst { it.toString() == uriString }
        if (index >= 0) {
            current.removeAt(index)
        } else if (current.size + reservedCount < MAX_TOTAL_PHOTOS) {
            current.add(uri)
        }
        _selectedUris.value = current
    }

    private fun loadPhotos() {
        viewModelScope.launch {
            _uiState.value = AlbumPickerUiState.Loading
            runCatching {
                val photos = withContext(Dispatchers.IO) { queryGallery() }
                AppLogger.d("ALBUM_PICKER", "Loaded ${photos.size} photos from gallery")
                _uiState.value = AlbumPickerUiState.Ready(photos)
            }.onFailure { e ->
                AppLogger.e("ALBUM_PICKER", "Gallery query failed: ${e.message}", e)
                _uiState.value = AlbumPickerUiState.Error("사진을 불러올 수 없습니다")
            }
        }
    }

    private fun queryGallery(): List<GalleryPhoto> {
        val photos = mutableListOf<GalleryPhoto>()
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_ADDED
        )
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        appContext.contentResolver.query(
            collection, projection, null, null, sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val dateAdded = cursor.getLong(dateColumn)
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )
                photos.add(GalleryPhoto(uri, dateAdded))
            }
        }
        return photos
    }
}
