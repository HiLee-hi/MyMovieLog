package com.mymovie.log.presentation.detail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mymovie.log.domain.model.Movie
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import java.time.LocalDate
import com.mymovie.log.domain.usecase.GetCurrentUserIdUseCase
import com.mymovie.log.domain.usecase.GetMovieDetailUseCase
import com.mymovie.log.domain.usecase.GetRecordByTmdbIdUseCase
import com.mymovie.log.domain.usecase.GetSignedPhotoUrlsUseCase
import com.mymovie.log.domain.usecase.UploadPhotosUseCase
import com.mymovie.log.domain.usecase.UpsertRecordUseCase
import com.mymovie.log.presentation.ui.AddRecordState
import com.mymovie.log.presentation.ui.PhotoAttachmentHost
import com.mymovie.log.presentation.ui.RecordDraft
import com.mymovie.log.util.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailUiState {
    object Loading : DetailUiState
    data class Success(val movie: Movie) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

@HiltViewModel
class MovieDetailViewModel @Inject constructor(
    private val getMovieDetailUseCase: GetMovieDetailUseCase,
    private val upsertRecordUseCase: UpsertRecordUseCase,
    private val uploadPhotosUseCase: UploadPhotosUseCase,
    private val getSignedPhotoUrlsUseCase: GetSignedPhotoUrlsUseCase,
    private val getCurrentUserIdUseCase: GetCurrentUserIdUseCase,
    private val getRecordByTmdbIdUseCase: GetRecordByTmdbIdUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel(), PhotoAttachmentHost {

    private val movieId: Int = checkNotNull(savedStateHandle["movieId"])

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    // The record editor state below is mirrored into SavedStateHandle so an in-progress record
    // survives configuration changes (fold/unfold, rotation, resize) and process recreation.

    /** Whether the record editor is open — rendered as a bottom sheet or as an inline pane. */
    private val _showBottomSheet = MutableStateFlow(savedStateHandle[KEY_EDITOR_OPEN] ?: false)
    val showBottomSheet: StateFlow<Boolean> = _showBottomSheet.asStateFlow()

    private val _addRecordState = MutableStateFlow<AddRecordState>(AddRecordState.Idle)
    val addRecordState: StateFlow<AddRecordState> = _addRecordState.asStateFlow()

    private val _recordDraft = MutableStateFlow(savedStateHandle[KEY_DRAFT] ?: RecordDraft())
    val recordDraft: StateFlow<RecordDraft> = _recordDraft.asStateFlow()

    private val _recordSavedThisSession = MutableStateFlow(savedStateHandle[KEY_RECORD_SAVED] ?: false)
    val recordSavedThisSession: StateFlow<Boolean> = _recordSavedThisSession.asStateFlow()

    private val _attachedUris = MutableStateFlow(
        savedStateHandle.get<List<String>>(KEY_ATTACHED_URIS).orEmpty().map(Uri::parse)
    )
    override val attachedUris: StateFlow<List<Uri>> = _attachedUris.asStateFlow()

    private val _keptExistingPhotoPaths = MutableStateFlow(
        savedStateHandle.get<List<String>>(KEY_KEPT_PHOTO_PATHS).orEmpty()
    )
    private val _keptExistingPhotoSourceUris = MutableStateFlow(
        savedStateHandle.get<List<String>>(KEY_KEPT_PHOTO_SOURCE_URIS).orEmpty()
    )
    override val existingPhotoSourceUris: StateFlow<List<String>> = _keptExistingPhotoSourceUris.asStateFlow()
    private val _existingPhotoSignedUrls = MutableStateFlow<List<String>>(emptyList())
    override val existingPhotoSignedUrls: StateFlow<List<String>> = _existingPhotoSignedUrls.asStateFlow()

    val existingRecord: StateFlow<MovieRecord?> = getRecordByTmdbIdUseCase(movieId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadDetail()
        // Signed URLs expire, so after recreation they are fetched again for the kept photos
        if (_showBottomSheet.value && _keptExistingPhotoPaths.value.isNotEmpty()) {
            loadSignedUrls(_keptExistingPhotoPaths.value)
        }
    }

    fun loadDetail() {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            AppLogger.d("VM_DETAIL", "Loading movie detail: movieId=$movieId")
            try {
                val movie = getMovieDetailUseCase(movieId)
                AppLogger.d("VM_DETAIL", "Movie detail loaded: title='${movie.title}'")
                _uiState.value = DetailUiState.Success(movie)
            } catch (e: Exception) {
                AppLogger.e("VM_DETAIL", "Failed to load movie detail: movieId=$movieId, error=${e.message}", e)
                _uiState.value = DetailUiState.Error(e.message ?: "불러오기 실패")
            }
        }
    }

    fun onRecordClick() {
        setEditorOpen(true)
        val record = existingRecord.value
        setDraft(RecordDraft.from(record))
        val paths = record?.photoUrls.orEmpty()
        setKeptExistingPhotos(paths, record?.photoSourceUris.orEmpty())
        _existingPhotoSignedUrls.value = emptyList()
        if (paths.isNotEmpty()) loadSignedUrls(paths)
    }

    fun onDraftChange(draft: RecordDraft) {
        setDraft(draft)
    }

    fun onDismissSheet() {
        setEditorOpen(false)
        _addRecordState.value = AddRecordState.Idle
        setDraft(RecordDraft())
        setAttachedUris(emptyList())
        setKeptExistingPhotos(emptyList(), emptyList())
        _existingPhotoSignedUrls.value = emptyList()
    }

    override fun addPhoto(uri: Uri) {
        val current = _attachedUris.value
        if (current.size < 10 && !current.contains(uri)) {
            setAttachedUris(current + uri)
        }
    }

    override fun setPhotos(uris: List<Uri>) {
        setAttachedUris(uris.take(10))
    }

    fun removePhoto(uri: Uri) {
        setAttachedUris(_attachedUris.value.filter { it != uri })
    }

    override fun removeExistingPhoto(signedUrl: String) {
        val index = _existingPhotoSignedUrls.value.indexOf(signedUrl)
        if (index >= 0) {
            setKeptExistingPhotos(
                _keptExistingPhotoPaths.value.filterIndexed { i, _ -> i != index },
                _keptExistingPhotoSourceUris.value.filterIndexed { i, _ -> i != index }
            )
            _existingPhotoSignedUrls.value = _existingPhotoSignedUrls.value.filterIndexed { i, _ -> i != index }
        }
    }

    private fun loadSignedUrls(paths: List<String>) {
        viewModelScope.launch {
            runCatching { getSignedPhotoUrlsUseCase(paths) }
                .onSuccess { _existingPhotoSignedUrls.value = it }
        }
    }

    private fun setEditorOpen(open: Boolean) {
        _showBottomSheet.value = open
        savedStateHandle[KEY_EDITOR_OPEN] = open
    }

    private fun setDraft(draft: RecordDraft) {
        _recordDraft.value = draft
        savedStateHandle[KEY_DRAFT] = draft
    }

    private fun setAttachedUris(uris: List<Uri>) {
        _attachedUris.value = uris
        savedStateHandle[KEY_ATTACHED_URIS] = ArrayList(uris.map(Uri::toString))
    }

    private fun setKeptExistingPhotos(paths: List<String>, sourceUris: List<String>) {
        _keptExistingPhotoPaths.value = paths
        _keptExistingPhotoSourceUris.value = sourceUris
        savedStateHandle[KEY_KEPT_PHOTO_PATHS] = ArrayList(paths)
        savedStateHandle[KEY_KEPT_PHOTO_SOURCE_URIS] = ArrayList(sourceUris)
    }

    fun saveRecord(
        movie: Movie,
        status: WatchStatus,
        rating: Float?,
        watchedAt: LocalDate?,
        review: String?,
        memo: String?
    ) {
        viewModelScope.launch {
            _addRecordState.value = AddRecordState.Saving
            try {
                val userId = getCurrentUserIdUseCase()
                val newPhotoPaths = if (_attachedUris.value.isNotEmpty()) {
                    AppLogger.i("VM_DETAIL", "Uploading ${_attachedUris.value.size} photos")
                    uploadPhotosUseCase(userId, movie.id, _attachedUris.value)
                } else emptyList()

                val record = MovieRecord(
                    id = existingRecord.value?.id ?: "",
                    userId = userId,
                    tmdbId = movie.id,
                    title = movie.title,
                    originalTitle = movie.originalTitle,
                    posterPath = movie.posterPath,
                    genreIds = movie.genreIds,
                    status = status,
                    rating = rating,
                    review = review?.takeIf { it.isNotBlank() },
                    memo = memo?.takeIf { it.isNotBlank() },
                    watchedAt = watchedAt,
                    photoUrls = _keptExistingPhotoPaths.value + newPhotoPaths,
                    photoSourceUris = _keptExistingPhotoSourceUris.value + _attachedUris.value.map { it.toString() }
                )
                AppLogger.i("VM_DETAIL", "Saving record: tmdbId=${movie.id}, status=${status.value}, photos=${record.photoUrls.size}")
                upsertRecordUseCase(record)
                AppLogger.i("VM_DETAIL", "Record saved successfully")
                _addRecordState.value = AddRecordState.Success
                _recordSavedThisSession.value = true
                savedStateHandle[KEY_RECORD_SAVED] = true
            } catch (e: Exception) {
                AppLogger.e("VM_DETAIL", "Save record failed: ${e.message}", e)
                _addRecordState.value = AddRecordState.Error(e.message ?: "저장 실패")
            }
        }
    }

    private companion object {
        const val KEY_EDITOR_OPEN = "record_editor_open"
        const val KEY_DRAFT = "record_draft"
        const val KEY_RECORD_SAVED = "record_saved_this_session"
        const val KEY_ATTACHED_URIS = "record_attached_uris"
        const val KEY_KEPT_PHOTO_PATHS = "record_kept_photo_paths"
        const val KEY_KEPT_PHOTO_SOURCE_URIS = "record_kept_photo_source_uris"
    }
}
