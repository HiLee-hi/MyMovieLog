package com.mymovie.log.presentation.picker

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumPickerScreen(
    alreadyAttachedUris: List<Uri>,
    existingPhotoCount: Int = 0,
    onConfirm: (List<Uri>) -> Unit,
    onBack: () -> Unit,
    viewModel: AlbumPickerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedUris by viewModel.selectedUris.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.initialize(alreadyAttachedUris, existingPhotoCount)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("사진 선택") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "닫기")
                    }
                },
                actions = {
                    Text(
                        text = "${selectedUris.size + existingPhotoCount}/10",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    TextButton(onClick = { onConfirm(selectedUris) }) {
                        Text(
                            text = if (selectedUris.isEmpty()) "완료" else "완료 (${selectedUris.size}장)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is AlbumPickerUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is AlbumPickerUiState.Ready -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(1.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(state.photos, key = { it.uri.toString() }) { photo ->
                        val uriStr = photo.uri.toString()
                        val index = selectedUris.indexOfFirst { it.toString() == uriStr }
                        val order = if (index >= 0) index + 1 else -1
                        val isSelected = order > 0
                        val isAtMax = (selectedUris.size + existingPhotoCount) >= 10 && !isSelected

                        PhotoGridItem(
                            uri = photo.uri,
                            selectionOrder = order,
                            isSelected = isSelected,
                            isAtMax = isAtMax,
                            onClick = { viewModel.toggleSelection(photo.uri) }
                        )
                    }
                }
            }

            is AlbumPickerUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun PhotoGridItem(
    uri: Uri,
    selectionOrder: Int,
    isSelected: Boolean,
    isAtMax: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = uri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (isAtMax) 0.4f else 1f)
        )

        if (isSelected) {
            // 선택 오버레이
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.25f))
            )
        }

        // 선택 인디케이터 (우상단)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .size(24.dp)
                .then(
                    if (isSelected) {
                        Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier.border(2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Text(
                    text = "$selectionOrder",
                    color = Color.White,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
