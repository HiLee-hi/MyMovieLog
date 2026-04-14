package com.mymovie.log.presentation.search

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mymovie.log.domain.model.Movie
import com.mymovie.log.domain.model.WatchStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    isLoggedIn: Boolean = true,
    onNavigateToLogin: () -> Unit = {},
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedMovie by viewModel.selectedMovie.collectAsStateWithLifecycle()
    val addRecordState by viewModel.addRecordState.collectAsStateWithLifecycle()
    var isSearchActive by remember { mutableStateOf(false) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Close the sheet on save success
    LaunchedEffect(addRecordState) {
        if (addRecordState is AddRecordState.Success) {
            sheetState.hide()
            viewModel.onDismissSheet()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SearchBar(
            query = query,
            onQueryChange = viewModel::onQueryChange,
            onSearch = { isSearchActive = false },
            active = isSearchActive,
            onActiveChange = { isSearchActive = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isSearchActive) 0.dp else 16.dp, vertical = 8.dp),
            placeholder = { Text("영화 제목으로 검색") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "지우기")
                    }
                }
            }
        ) {
            SearchResultContent(uiState = uiState, onMovieClick = { movie ->
                if (isLoggedIn) viewModel.onMovieClick(movie) else onNavigateToLogin()
            })
        }

        if (!isSearchActive) {
            SearchResultContent(uiState = uiState, onMovieClick = { movie ->
                if (isLoggedIn) viewModel.onMovieClick(movie) else onNavigateToLogin()
            })
        }
    }

    if (selectedMovie != null) {
        AddRecordBottomSheet(
            movie = selectedMovie!!,
            addRecordState = addRecordState,
            sheetState = sheetState,
            onDismiss = viewModel::onDismissSheet,
            onSave = { status, rating, watchedAt, review, memo ->
                viewModel.saveRecord(selectedMovie!!, status, rating, watchedAt, review, memo)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddRecordBottomSheet(
    movie: Movie,
    addRecordState: AddRecordState,
    sheetState: androidx.compose.material3.SheetState,
    onDismiss: () -> Unit,
    onSave: (WatchStatus, Float?, LocalDate?, String?, String?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf(WatchStatus.WATCHED) }
    var rating by remember { mutableFloatStateOf(0f) }
    var watchedAt by remember { mutableStateOf<LocalDate?>(null) }
    var review by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }

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
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Movie title
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

            // Watch status selector (Watched / Wishlist)
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

            // Fields shown only for WATCHED status
            if (selectedStatus == WatchStatus.WATCHED) {
                // Star rating
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

                // Watch date picker
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

                // Review input
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
                // Memo input shown only for WISHLIST status
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
                    Text("기록 저장")
                }
            }
        }
    }
}

@Composable
private fun StarRatingRow(
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
                        // cycle: full star → half star → previous star
                        onRatingChange(if (rating == star.toFloat()) star - 0.5f else star.toFloat())
                    }
            )
        }
    }
}

@Composable
private fun SearchResultContent(uiState: SearchUiState, onMovieClick: (Movie) -> Unit) {
    when (uiState) {
        is SearchUiState.Idle -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("영화를 검색해보세요", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        is SearchUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is SearchUiState.Success -> {
            if (uiState.movies.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("검색 결과가 없어요", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.movies) { movie ->
                        MovieSearchItem(movie = movie, onClick = { onMovieClick(movie) })
                    }
                }
            }
        }
        is SearchUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("오류: ${uiState.message}", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun MovieSearchItem(movie: Movie, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = movie.posterUrl,
            contentDescription = movie.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(60.dp, 90.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = movie.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
            if (movie.originalTitle != movie.title) {
                Text(text = movie.originalTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = movie.releaseDate.take(4), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = movie.overview, style = MaterialTheme.typography.bodySmall, maxLines = 3, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
