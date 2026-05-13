package com.mymovie.log.presentation.ui

import android.app.DatePickerDialog
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mymovie.log.domain.model.Movie
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

sealed class AddRecordState {
    object Idle : AddRecordState()
    object Saving : AddRecordState()
    object Success : AddRecordState()
    data class Error(val message: String) : AddRecordState()
}

private const val MAX_PHOTOS = 10
private val THUMBNAIL_SIZE = 72.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddRecordBottomSheet(
    movie: Movie,
    existingRecord: MovieRecord? = null,
    existingPhotoSignedUrls: List<String> = emptyList(),
    addRecordState: AddRecordState,
    attachedUris: List<Uri> = emptyList(),
    onOpenCamera: () -> Unit = {},
    onOpenAlbumPicker: () -> Unit = {},
    onRemovePhoto: (Uri) -> Unit = {},
    onRemoveExistingPhoto: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (WatchStatus, Float?, LocalDate?, String?, String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(addRecordState) {
        if (addRecordState is AddRecordState.Success) {
            sheetState.hide()
            onDismiss()
        }
    }

    var selectedStatus by remember { mutableStateOf(existingRecord?.status ?: WatchStatus.WATCHED) }
    var rating by remember { mutableFloatStateOf(existingRecord?.rating ?: 0f) }
    var watchedAt by remember { mutableStateOf(existingRecord?.watchedAt) }
    var review by remember { mutableStateOf(existingRecord?.review ?: "") }
    var memo by remember { mutableStateOf(existingRecord?.memo ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    // existingRecord가 나중에 로드되는 경우(DB 응답 지연) 필드 동기화
    LaunchedEffect(existingRecord) {
        existingRecord?.let { record ->
            selectedStatus = record.status
            rating = record.rating ?: 0f
            watchedAt = record.watchedAt
            review = record.review ?: ""
            memo = record.memo ?: ""
        }
    }

    val context = LocalContext.current
    val isSaving = addRecordState is AddRecordState.Saving
    val errorMessage = (addRecordState as? AddRecordState.Error)?.message

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = movie.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            if (movie.originalTitle != movie.title) {
                Text(
                    text = movie.originalTitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedStatus == WatchStatus.WATCHED,
                    onClick = { selectedStatus = WatchStatus.WATCHED },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) { Text("봤어요") }
                SegmentedButton(
                    selected = selectedStatus == WatchStatus.WISHLIST,
                    onClick = { selectedStatus = WatchStatus.WISHLIST },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) { Text("보고싶어요") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedStatus == WatchStatus.WATCHED) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("별점", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(48.dp))
                    StarRatingRow(
                        rating = rating,
                        onRatingChange = { rating = it },
                        modifier = Modifier.weight(1f)
                    )
                    if (rating > 0f) {
                        TextButton(onClick = { rating = 0f }) {
                            Text("없음", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 M월 d일") }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = watchedAt?.format(dateFormatter) ?: "",
                        onValueChange = {},
                        label = { Text("감상한 날짜") },
                        modifier = Modifier.fillMaxWidth(),
                        readOnly = true,
                        trailingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) }
                    )
                    Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                }

                if (showDatePicker) {
                    val today = LocalDate.now()
                    val initial = watchedAt ?: today
                    val dialog = remember(initial) {
                        DatePickerDialog(
                            context,
                            { _, year, month, day ->
                                watchedAt = LocalDate.of(year, month + 1, day)
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
                    value = review,
                    onValueChange = { review = it },
                    label = { Text("리뷰 (선택)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    enabled = !isSaving
                )
            } else {
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
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
                existingPhotoUrls = existingPhotoSignedUrls,
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
                    onSave(
                        selectedStatus,
                        if (selectedStatus == WatchStatus.WATCHED && rating > 0f) rating else null,
                        if (selectedStatus == WatchStatus.WATCHED) watchedAt else null,
                        if (selectedStatus == WatchStatus.WATCHED) review else null,
                        if (selectedStatus == WatchStatus.WISHLIST) memo else null
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
                    Text("기록 저장")
                }
            }
        }
    }
}

@Composable
internal fun PhotoAttachSection(
    attachedUris: List<Uri>,
    existingPhotoUrls: List<String> = emptyList(),
    onOpenCamera: () -> Unit,
    onOpenAlbumPicker: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    onRemoveExistingPhoto: (String) -> Unit = {},
    enabled: Boolean
) {
    val totalPhotoCount = existingPhotoUrls.size + attachedUris.size
    val canAddMore = totalPhotoCount < MAX_PHOTOS

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("사진", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                text = "$totalPhotoCount / $MAX_PHOTOS",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (canAddMore) {
                PhotoActionButton(
                    icon = Icons.Default.PhotoCamera,
                    label = "카메라",
                    enabled = enabled,
                    onClick = onOpenCamera
                )
                PhotoActionButton(
                    icon = Icons.Default.PhotoLibrary,
                    label = "앨범",
                    enabled = enabled,
                    onClick = onOpenAlbumPicker
                )
            }
            attachedUris.forEach { uri ->
                PhotoThumbnail(
                    uri = uri,
                    enabled = enabled,
                    onRemove = { onRemovePhoto(uri) }
                )
            }
            existingPhotoUrls.forEach { url ->
                ExistingPhotoThumbnail(
                    url = url,
                    enabled = enabled,
                    onRemove = { onRemoveExistingPhoto(url) }
                )
            }
        }
    }
}

@Composable
internal fun PhotoActionButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(THUMBNAIL_SIZE),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
internal fun PhotoThumbnail(
    uri: Uri,
    enabled: Boolean,
    onRemove: () -> Unit
) {
    Box(modifier = Modifier.size(THUMBNAIL_SIZE)) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(18.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .then(if (enabled) Modifier.clickable(onClick = onRemove) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "사진 제거",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

@Composable
internal fun ExistingPhotoThumbnail(
    url: String,
    enabled: Boolean = true,
    onRemove: () -> Unit = {}
) {
    Box(modifier = Modifier.size(THUMBNAIL_SIZE)) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(3.dp)
                .size(18.dp)
                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                .then(if (enabled) Modifier.clickable(onClick = onRemove) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "사진 제거",
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
        }
    }
}
