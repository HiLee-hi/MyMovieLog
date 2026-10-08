package com.mymovie.log.presentation.ui

import android.app.DatePickerDialog
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import java.io.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * In-progress record form values. Held by ViewModels (never by the form composable) so a draft
 * survives fold/unfold, rotation, window resizing and the switch between the bottom sheet and
 * the inline pane. [Serializable] so it can also be kept in a SavedStateHandle.
 */
data class RecordDraft(
    val status: WatchStatus = WatchStatus.WATCHED,
    val rating: Float = 0f,
    val watchedAt: LocalDate? = null,
    val review: String = "",
    val memo: String = ""
) : Serializable {
    companion object {
        fun from(record: MovieRecord?): RecordDraft = record?.let {
            RecordDraft(
                status = it.status,
                rating = it.rating ?: 0f,
                watchedAt = it.watchedAt,
                review = it.review ?: "",
                memo = it.memo ?: ""
            )
        } ?: RecordDraft()
    }
}

/**
 * The record form (status, rating, watched date, review/memo, photos, save), without any
 * container. Used inside the bottom sheets on compact windows and inline next to the movie
 * information on wide windows.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordEditorForm(
    draft: RecordDraft,
    onDraftChange: (RecordDraft) -> Unit,
    saveState: AddRecordState,
    saveLabel: String,
    onSave: (WatchStatus, Float?, LocalDate?, String?, String?) -> Unit,
    modifier: Modifier = Modifier,
    attachedUris: List<Uri> = emptyList(),
    existingPhotoUrls: List<String> = emptyList(),
    onOpenCamera: () -> Unit = {},
    onOpenAlbumPicker: () -> Unit = {},
    onRemovePhoto: (Uri) -> Unit = {},
    onRemoveExistingPhoto: (String) -> Unit = {},
) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val context = LocalContext.current
    val isSaving = saveState is AddRecordState.Saving
    val errorMessage = (saveState as? AddRecordState.Error)?.message

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SegmentedButton(
                selected = draft.status == WatchStatus.WATCHED,
                onClick = { onDraftChange(draft.copy(status = WatchStatus.WATCHED)) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) { Text("봤어요") }
            SegmentedButton(
                selected = draft.status == WatchStatus.WISHLIST,
                onClick = { onDraftChange(draft.copy(status = WatchStatus.WISHLIST)) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) { Text("보고싶어요") }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (draft.status == WatchStatus.WATCHED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("별점", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(48.dp))
                StarRatingRow(
                    rating = draft.rating,
                    onRatingChange = { onDraftChange(draft.copy(rating = it)) },
                    modifier = Modifier.weight(1f)
                )
                if (draft.rating > 0f) {
                    TextButton(onClick = { onDraftChange(draft.copy(rating = 0f)) }) {
                        Text("없음", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 M월 d일") }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = draft.watchedAt?.format(dateFormatter) ?: "",
                    onValueChange = {},
                    label = { Text("감상한 날짜") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                )
                Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
            }

            if (showDatePicker) {
                val initial = draft.watchedAt ?: LocalDate.now()
                val currentDraft by rememberUpdatedState(draft)
                val currentOnDraftChange by rememberUpdatedState(onDraftChange)
                val dialog = remember(initial) {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            currentOnDraftChange(currentDraft.copy(watchedAt = LocalDate.of(year, month + 1, day)))
                        },
                        initial.year, initial.monthValue - 1, initial.dayOfMonth
                    ).apply {
                        setOnDismissListener { showDatePicker = false }
                    }
                }
                DisposableEffect(dialog) {
                    dialog.show()
                    onDispose { if (dialog.isShowing) dialog.dismiss() }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = draft.review,
                onValueChange = { onDraftChange(draft.copy(review = it)) },
                label = { Text("리뷰 (선택)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                enabled = !isSaving
            )
        } else {
            OutlinedTextField(
                value = draft.memo,
                onValueChange = { onDraftChange(draft.copy(memo = it)) },
                label = { Text("메모 (선택)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                enabled = !isSaving
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        PhotoAttachSection(
            attachedUris = attachedUris,
            existingPhotoUrls = existingPhotoUrls,
            onOpenCamera = onOpenCamera,
            onOpenAlbumPicker = onOpenAlbumPicker,
            onRemovePhoto = onRemovePhoto,
            onRemoveExistingPhoto = onRemoveExistingPhoto,
            enabled = !isSaving
        )

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                val watched = draft.status == WatchStatus.WATCHED
                onSave(
                    draft.status,
                    if (watched && draft.rating > 0f) draft.rating else null,
                    if (watched) draft.watchedAt else null,
                    if (watched) draft.review else null,
                    if (!watched) draft.memo else null
                )
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isSaving
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(saveLabel)
            }
        }
    }
}
