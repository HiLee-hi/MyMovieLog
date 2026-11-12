package com.mymovie.log.presentation.ui

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailBottomSheet(
    record: MovieRecord,
    draft: RecordDraft,
    onDraftChange: (RecordDraft) -> Unit,
    editState: AddRecordState,
    attachedUris: List<Uri> = emptyList(),
    existingPhotoSignedUrls: List<String> = emptyList(),
    onOpenCamera: () -> Unit = {},
    onOpenAlbumPicker: () -> Unit = {},
    onRemovePhoto: (Uri) -> Unit = {},
    onRemoveExistingPhoto: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (WatchStatus, Float?, LocalDate?, String?, String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(editState) {
        if (editState is AddRecordState.Success) {
            sheetState.hide()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        RecordDetailContent(
            record = record,
            draft = draft,
            onDraftChange = onDraftChange,
            editState = editState,
            attachedUris = attachedUris,
            existingPhotoSignedUrls = existingPhotoSignedUrls,
            onOpenCamera = onOpenCamera,
            onOpenAlbumPicker = onOpenAlbumPicker,
            onRemovePhoto = onRemovePhoto,
            onRemoveExistingPhoto = onRemoveExistingPhoto,
            onSave = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        )
    }
}

/** Poster/title header plus the record form. Shown in the sheet or in a pane on large windows. */
@Composable
fun RecordDetailContent(
    record: MovieRecord,
    draft: RecordDraft,
    onDraftChange: (RecordDraft) -> Unit,
    editState: AddRecordState,
    onSave: (WatchStatus, Float?, LocalDate?, String?, String?) -> Unit,
    modifier: Modifier = Modifier,
    attachedUris: List<Uri> = emptyList(),
    existingPhotoSignedUrls: List<String> = emptyList(),
    onOpenCamera: () -> Unit = {},
    onOpenAlbumPicker: () -> Unit = {},
    onRemovePhoto: (Uri) -> Unit = {},
    onRemoveExistingPhoto: (String) -> Unit = {},
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        // Poster + title header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AsyncImage(
                model = record.posterUrl,
                contentDescription = record.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(width = 60.dp, height = 90.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
            Column {
                Text(
                    text = record.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (record.originalTitle.isNotBlank() && record.originalTitle != record.title) {
                    Text(
                        text = record.originalTitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        RecordEditorForm(
            draft = draft,
            onDraftChange = onDraftChange,
            saveState = editState,
            saveLabel = "저장",
            onSave = onSave,
            attachedUris = attachedUris,
            existingPhotoUrls = existingPhotoSignedUrls,
            onOpenCamera = onOpenCamera,
            onOpenAlbumPicker = onOpenAlbumPicker,
            onRemovePhoto = onRemovePhoto,
            onRemoveExistingPhoto = onRemoveExistingPhoto
        )
    }
}
