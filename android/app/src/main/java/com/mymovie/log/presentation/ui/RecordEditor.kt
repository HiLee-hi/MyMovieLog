package com.mymovie.log.presentation.ui

import android.app.DatePickerDialog
import com.mymovie.log.presentation.ui.theme.popcornButtonColors
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mymovie.log.R
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import androidx.compose.ui.semantics.Role
import java.io.Serializable
import java.time.LocalDate

// draft held by ViewModels, not the composable, so it survives fold / rotate / resize / sheet <-> pane
// Serializable for SavedStateHandle
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

// form only, no container: inside sheets on compact, inline on wide
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
    val errorMessage = (saveState as? AddRecordState.Error)?.message?.asString()

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        PopcornToggle(
            options = listOf(WatchStatus.WATCHED, WatchStatus.WISHLIST),
            selected = draft.status,
            onSelect = { onDraftChange(draft.copy(status = it)) },
            label = { status ->
                if (status == WatchStatus.WATCHED) stringResource(R.string.record_status_watched)
                else stringResource(R.string.record_status_wishlist)
            },
            role = Role.RadioButton
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (draft.status == WatchStatus.WATCHED) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.record_rating), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(48.dp))
                StarRatingRow(
                    rating = draft.rating,
                    onRatingChange = { onDraftChange(draft.copy(rating = it)) },
                    modifier = Modifier.weight(1f)
                )
                if (draft.rating > 0f) {
                    TextButton(onClick = { onDraftChange(draft.copy(rating = 0f)) }) {
                        Text(stringResource(R.string.record_rating_none), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            val dateFormatter = rememberDateTimeFormatter(R.string.format_date_full)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = draft.watchedAt?.format(dateFormatter) ?: "",
                    onValueChange = {},
                    label = { Text(stringResource(R.string.record_watched_date)) },
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
                label = { Text(stringResource(R.string.record_review_optional)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                enabled = !isSaving
            )
        } else {
            OutlinedTextField(
                value = draft.memo,
                onValueChange = { onDraftChange(draft.copy(memo = it)) },
                label = { Text(stringResource(R.string.record_memo_optional)) },
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
            colors = popcornButtonColors(),
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
