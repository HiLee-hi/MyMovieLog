package com.mymovie.log.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCalendar: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("MyMovieLog", fontWeight = FontWeight.Bold) },
            actions = {
                IconButton(onClick = onNavigateToCalendar) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "캘린더")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            // Stats summary cards
            StatsSummaryRow(
                totalWatched = uiState.totalWatched,
                thisMonthCount = uiState.thisMonthCount
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Recently watched section
            SectionHeader(title = "최근에 본 영화")
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.recentWatched.isEmpty()) {
                EmptyMessage("아직 감상 기록이 없어요. 영화를 검색해 기록해보세요!")
            } else {
                MoviePosterRow(records = uiState.recentWatched)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Wishlist preview section
            SectionHeader(title = "보고 싶은 영화")
            Spacer(modifier = Modifier.height(8.dp))
            if (uiState.wishlistPreview.isEmpty()) {
                EmptyMessage("위시리스트가 비어있어요. 보고 싶은 영화를 추가해보세요!")
            } else {
                MoviePosterRow(records = uiState.wishlistPreview)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatsSummaryRow(totalWatched: Int, thisMonthCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "총 감상",
            value = "${totalWatched}편"
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "이번 달",
            value = "${thisMonthCount}편"
        )
    }
}

@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, value: String) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun EmptyMessage(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MoviePosterRow(records: List<MovieRecord>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 8.dp)
    ) {
        items(records) { record ->
            MoviePosterItem(record = record)
        }
    }
}

@Composable
private fun MoviePosterItem(record: MovieRecord, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = record.posterUrl,
            contentDescription = record.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(100.dp, 150.dp)
                .clip(RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = record.title,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2
        )
    }
}
