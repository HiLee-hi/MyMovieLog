package com.mymovie.log.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.presentation.ui.LoginRequiredContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    isLoggedIn: Boolean = true,
    onNavigateToLogin: () -> Unit = {},
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val watchedRecords by viewModel.watchedRecords.collectAsStateWithLifecycle()
    val wishlistRecords by viewModel.wishlistRecords.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("라이브러리", fontWeight = FontWeight.Bold) })

        if (!isLoggedIn) {
            LoginRequiredContent(
                message = "내 영화 목록을 보려면\n로그인이 필요해요",
                onNavigateToLogin = onNavigateToLogin
            )
            return@Column
        }

        TabRow(selectedTabIndex = if (selectedTab == WatchStatus.WATCHED) 0 else 1) {
            Tab(
                selected = selectedTab == WatchStatus.WATCHED,
                onClick = { viewModel.selectTab(WatchStatus.WATCHED) },
                text = { Text("감상 완료 (${watchedRecords.size})") }
            )
            Tab(
                selected = selectedTab == WatchStatus.WISHLIST,
                onClick = { viewModel.selectTab(WatchStatus.WISHLIST) },
                text = { Text("위시리스트 (${wishlistRecords.size})") }
            )
        }

        val currentRecords = if (selectedTab == WatchStatus.WATCHED) watchedRecords else wishlistRecords

        if (currentRecords.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (selectedTab == WatchStatus.WATCHED) "감상 기록이 없어요" else "위시리스트가 비어있어요",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            MovieGrid(records = currentRecords)
        }
    }
}

@Composable
private fun MovieGrid(records: List<MovieRecord>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(records) { record ->
            MovieGridItem(record = record)
        }
    }
}

@Composable
private fun MovieGridItem(record: MovieRecord, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = record.posterUrl,
            contentDescription = record.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .size(width = 110.dp, height = 165.dp)
                .clip(RoundedCornerShape(6.dp))
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
