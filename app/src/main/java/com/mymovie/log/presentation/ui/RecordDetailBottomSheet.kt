package com.mymovie.log.presentation.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailBottomSheet(
    record: MovieRecord,
    editState: AddRecordState,
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

    var selectedStatus by remember { mutableStateOf(record.status) }
    var rating by remember { mutableFloatStateOf(record.rating ?: 0f) }
    var watchedAt by remember { mutableStateOf(record.watchedAt) }
    var review by remember { mutableStateOf(record.review ?: "") }
    var memo by remember { mutableStateOf(record.memo ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isSaving = editState is AddRecordState.Saving
    val errorMessage = (editState as? AddRecordState.Error)?.message
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy년 M월 d일") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
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

            // Watch status selector
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
                // Star rating
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("별점", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(48.dp))
                    DetailStarRatingRow(
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

                // Watch date picker
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
                    Text("저장")
                }
            }
        }
    }
}

@Composable
private fun DetailStarRatingRow(
    rating: Float,
    onRatingChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (star in 1..5) {
            val filled = rating >= star
            val halfFilled = !filled && rating >= star - 0.5f
            Icon(
                imageVector = when {
                    filled -> Icons.Default.Star
                    halfFilled -> Icons.Default.StarHalf
                    else -> Icons.Outlined.StarBorder
                },
                contentDescription = "$star 점",
                tint = if (filled || halfFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                modifier = Modifier
                    .size(32.dp)
                    .clickable {
                        onRatingChange(if (rating == star.toFloat()) star - 0.5f else star.toFloat())
                    }
            )
        }
    }
}
