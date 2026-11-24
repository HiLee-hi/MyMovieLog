package com.mymovie.log.presentation.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.semantics.Role
import com.mymovie.log.presentation.ui.PopcornToggle
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import android.net.Uri
import androidx.compose.ui.res.stringResource
import com.mymovie.log.R
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.presentation.ui.LoginRequiredContent
import com.mymovie.log.presentation.ui.RecordDetailBottomSheet
import com.mymovie.log.presentation.adaptive.AdaptiveDimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    isLoggedIn: Boolean = true,
    onNavigateToLogin: () -> Unit = {},
    onOpenCamera: () -> Unit = {},
    onOpenAlbumPicker: (List<Uri>) -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val watchedRecords by viewModel.watchedRecords.collectAsStateWithLifecycle()
    val wishlistRecords by viewModel.wishlistRecords.collectAsStateWithLifecycle()
    val selectedRecord by viewModel.selectedRecord.collectAsStateWithLifecycle()
    val editRecordState by viewModel.editRecordState.collectAsStateWithLifecycle()
    val editDraft by viewModel.editDraft.collectAsStateWithLifecycle()
    val attachedUris by viewModel.attachedUris.collectAsStateWithLifecycle()
    val selectedRecordSignedPhotoUrls by viewModel.existingPhotoSignedUrls.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.nav_library), fontWeight = FontWeight.Bold) })

        if (!isLoggedIn) {
            LoginRequiredContent(
                message = stringResource(R.string.library_login_required),
                onNavigateToLogin = onNavigateToLogin
            )
            return@Column
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            PopcornToggle(
                options = listOf(WatchStatus.WATCHED, WatchStatus.WISHLIST),
                selected = selectedTab,
                onSelect = viewModel::selectTab,
                label = { status ->
                    if (status == WatchStatus.WATCHED) stringResource(R.string.library_watched_tab, watchedRecords.size)
                    else stringResource(R.string.library_wishlist_tab, wishlistRecords.size)
                },
                role = Role.Tab,
                modifier = Modifier
                    .widthIn(max = LibraryTabsMaxWidth)
                    .padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
            )
        }

        val currentRecords = if (selectedTab == WatchStatus.WATCHED) watchedRecords else wishlistRecords

        if (currentRecords.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (selectedTab == WatchStatus.WATCHED) stringResource(R.string.library_watched_empty) else stringResource(R.string.library_wishlist_empty),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            MovieGrid(
                records = currentRecords,
                onRecordClick = viewModel::selectRecord,
                onDelete = viewModel::deleteRecord
            )
        }
    }

    selectedRecord?.let { record ->
        RecordDetailBottomSheet(
            record = record,
            draft = editDraft,
            onDraftChange = viewModel::onEditDraftChange,
            editState = editRecordState,
            attachedUris = attachedUris,
            existingPhotoSignedUrls = selectedRecordSignedPhotoUrls,
            onOpenCamera = onOpenCamera,
            onOpenAlbumPicker = { onOpenAlbumPicker(attachedUris) },
            onRemovePhoto = viewModel::removePhoto,
            onRemoveExistingPhoto = viewModel::removeExistingPhoto,
            onDismiss = viewModel::clearSelectedRecord,
            onSave = viewModel::updateRecord
        )
    }
}

@Composable
private fun MovieGrid(
    records: List<MovieRecord>,
    onRecordClick: (MovieRecord) -> Unit,
    onDelete: (String) -> Unit
) {
    // Column count follows the available width: 3 on phones, more on unfolded / tablet windows
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = AdaptiveDimens.PosterGridMinCellWidth),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(records, key = { it.id }) { record ->
            MovieGridItem(
                record = record,
                onClick = { onRecordClick(record) },
                onDelete = { onDelete(record.id) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MovieGridItem(record: MovieRecord, onClick: () -> Unit = {}, onDelete: () -> Unit = {}) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) showDeleteDialog = true
            false
        }
    )

    LaunchedEffect(showDeleteDialog) {
        if (!showDeleteDialog) dismissState.reset()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.record_delete_title)) },
            text = { Text(stringResource(R.string.record_delete_confirm, record.title)) },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text(stringResource(R.string.common_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.common_cancel)) }
            }
        )
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.common_delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    ) {
        // Opaque, so the delete background only shows while swiping
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .clickable { onClick() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = record.posterUrl,
                contentDescription = record.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            record.rating?.let { rating ->
                Text(
                    text = "★ $rating",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/** The tabs keep their phone proportions instead of stretching across wide windows. */
private val LibraryTabsMaxWidth = 480.dp
