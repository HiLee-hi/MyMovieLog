package com.mymovie.log.presentation.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.domain.repository.AuthRepository
import com.mymovie.log.domain.usecase.GetRecordsUseCase
import com.mymovie.log.domain.usecase.GetSignedPhotoUrlsUseCase
import com.mymovie.log.domain.usecase.UploadPhotosUseCase
import com.mymovie.log.domain.usecase.UpsertRecordUseCase
import com.mymovie.log.presentation.ui.AddRecordState
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class HomeUiState(
    val recentWatched: List<MovieRecord> = emptyList(),
    val wishlistPreview: List<MovieRecord> = emptyList(),
    val totalWatched: Int = 0,
    val thisMonthCount: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    getRecordsUseCase: GetRecordsUseCase,
    private val upsertRecordUseCase: UpsertRecordUseCase,
    private val uploadPhotosUseCase: UploadPhotosUseCase,
    private val getSignedPhotoUrlsUseCase: GetSignedPhotoUrlsUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = getRecordsUseCase()
        .map { allRecords ->
            val watched = allRecords.filter { it.status == WatchStatus.WATCHED }
            val wishlist = allRecords.filter { it.status == WatchStatus.WISHLIST }
            val thisMonth = java.time.YearMonth.now()
            val thisMonthCount = watched.count { record ->
                record.watchedAt?.let {
                    java.time.YearMonth.from(it) == thisMonth
                } ?: false
            }
            HomeUiState(
                recentWatched = watched.take(5),
                wishlistPreview = wishlist.take(5),
                totalWatched = watched.size,
                thisMonthCount = thisMonthCount
            )
        }
        .onEach { state ->
            AppLogger.d("VM_HOME", "UiState updated: totalWatched=${state.totalWatched}, thisMonth=${state.thisMonthCount}, wishlist=${state.wishlistPreview.size}")
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    private val _selectedRecord = MutableStateFlow<MovieRecord?>(null)
    val selectedRecord: StateFlow<MovieRecord?> = _selectedRecord.asStateFlow()

    private val _editRecordState = MutableStateFlow<AddRecordState>(AddRecordState.Idle)
    val editRecordState: StateFlow<AddRecordState> = _editRecordState.asStateFlow()

    private val _attachedUris = MutableStateFlow<List<Uri>>(emptyList())
    val attachedUris: StateFlow<List<Uri>> = _attachedUris.asStateFlow()

    private val _keptExistingPhotoPaths = MutableStateFlow<List<String>>(emptyList())
    private val _existingPhotoSignedUrls = MutableStateFlow<List<String>>(emptyList())
    val existingPhotoSignedUrls: StateFlow<List<String>> = _existingPhotoSignedUrls.asStateFlow()

    fun selectRecord(record: MovieRecord) {
        AppLogger.d("VM_HOME", "Record selected: id=${AppLogger.shortId(record.id)}")
        _selectedRecord.value = record
        _editRecordState.value = AddRecordState.Idle
        _attachedUris.value = emptyList()
        _keptExistingPhotoPaths.value = record.photoUrls
        _existingPhotoSignedUrls.value = emptyList()
        if (record.photoUrls.isNotEmpty()) {
            viewModelScope.launch {
                runCatching { getSignedPhotoUrlsUseCase(record.photoUrls) }
                    .onSuccess { _existingPhotoSignedUrls.value = it }
            }
        }
    }

    fun clearSelectedRecord() {
        _selectedRecord.value = null
        _editRecordState.value = AddRecordState.Idle
        _attachedUris.value = emptyList()
        _keptExistingPhotoPaths.value = emptyList()
        _existingPhotoSignedUrls.value = emptyList()
    }

    fun addPhoto(uri: Uri) {
        val current = _attachedUris.value
        if (current.size < 10 && !current.contains(uri)) {
            _attachedUris.value = current + uri
        }
    }

    fun setPhotos(uris: List<Uri>) {
        _attachedUris.value = uris.take(10)
    }

    fun removePhoto(uri: Uri) {
        _attachedUris.value = _attachedUris.value.filter { it != uri }
    }

    fun removeExistingPhoto(signedUrl: String) {
        val index = _existingPhotoSignedUrls.value.indexOf(signedUrl)
        if (index >= 0) {
            _keptExistingPhotoPaths.value = _keptExistingPhotoPaths.value.filterIndexed { i, _ -> i != index }
            _existingPhotoSignedUrls.value = _existingPhotoSignedUrls.value.filterIndexed { i, _ -> i != index }
        }
    }

    fun updateRecord(
        status: WatchStatus,
        rating: Float?,
        watchedAt: LocalDate?,
        review: String?,
        memo: String?
    ) {
        val record = _selectedRecord.value ?: return
        viewModelScope.launch {
            _editRecordState.value = AddRecordState.Saving
            AppLogger.i("VM_HOME", "Update record: id=${AppLogger.shortId(record.id)}, status=${status.value}")
            runCatching {
                val userId = authRepository.currentUser.first()?.id ?: ""
                val newPhotoPaths = if (_attachedUris.value.isNotEmpty()) {
                    AppLogger.i("VM_HOME", "Uploading ${_attachedUris.value.size} photos")
                    uploadPhotosUseCase(userId, record.tmdbId, _attachedUris.value)
                } else emptyList()

                upsertRecordUseCase(
                    record.copy(
                        status = status,
                        rating = rating,
                        watchedAt = watchedAt,
                        review = review?.takeIf { it.isNotBlank() },
                        memo = memo?.takeIf { it.isNotBlank() },
                        photoUrls = _keptExistingPhotoPaths.value + newPhotoPaths
                    )
                )
            }
                .onSuccess {
                    AppLogger.i("VM_HOME", "Update record success: id=${AppLogger.shortId(record.id)}")
                    _editRecordState.value = AddRecordState.Success
                }
                .onFailure {
                    AppLogger.e("VM_HOME", "Update record failed: ${it.message}", it)
                    _editRecordState.value = AddRecordState.Error(it.message ?: "저장 실패")
                }
        }
    }
}
