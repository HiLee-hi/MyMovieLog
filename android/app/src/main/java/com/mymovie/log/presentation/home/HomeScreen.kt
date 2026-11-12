package com.mymovie.log.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.mymovie.log.presentation.ui.theme.PopcornColors
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import android.net.Uri
import com.mymovie.log.R
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.presentation.ui.RecordDetailBottomSheet
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.testTag
import com.mymovie.log.presentation.adaptive.AdaptiveDimens
import com.mymovie.log.presentation.adaptive.AdaptivePreviewSurface
import com.mymovie.log.presentation.adaptive.AdaptiveTwoColumn
import com.mymovie.log.presentation.adaptive.AdaptiveWindowPreviews
import com.mymovie.log.presentation.adaptive.constrainedWidth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToCalendar: () -> Unit,
    onNavigateToLibraryTab: (WatchStatus) -> Unit = {},
    onOpenCamera: () -> Unit = {},
    onOpenAlbumPicker: (List<Uri>) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedRecord by viewModel.selectedRecord.collectAsStateWithLifecycle()
    val editRecordState by viewModel.editRecordState.collectAsStateWithLifecycle()
    val editDraft by viewModel.editDraft.collectAsStateWithLifecycle()
    val attachedUris by viewModel.attachedUris.collectAsStateWithLifecycle()
    val selectedRecordSignedPhotoUrls by viewModel.existingPhotoSignedUrls.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        HomeHeader(onNavigateToCalendar = onNavigateToCalendar)

        HomeContent(
            uiState = uiState,
            onRecordClick = viewModel::selectRecord,
            onNavigateToLibraryTab = onNavigateToLibraryTab,
            onNavigateToCalendar = onNavigateToCalendar
        )
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

/**
 * Compact: summary cards above the poster rows (original layout).
 * Wide: poster rows | summary side by side, capped at [AdaptiveDimens.ContentMaxWidth] so the
 * cards do not stretch across a 4:3 inner display or a tablet.
 */
@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onRecordClick: (MovieRecord) -> Unit,
    onNavigateToLibraryTab: (WatchStatus) -> Unit,
    onNavigateToCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = AdaptiveDimens.fitsTwoColumns(maxWidth - HorizontalPadding * 2)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .constrainedWidth()
                    .padding(horizontal = HorizontalPadding)
            ) {
                Text(
                    text = "오늘은 어떤 영화를 봤나요?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                if (isWide) {
                    AdaptiveTwoColumn(
                        modifier = Modifier.padding(top = 8.dp).testTag(HomeTestTags.TwoColumn),
                        firstFraction = 0.62f,
                        first = {
                            RecordSections(uiState, onRecordClick, onNavigateToLibraryTab)
                        },
                        second = {
                            StatsSummaryPanel(
                                totalWatched = uiState.totalWatched,
                                thisMonthCount = uiState.thisMonthCount,
                                onNavigateToCalendar = onNavigateToCalendar
                            )
                        }
                    )
                } else {
                    StatsSummaryRow(
                        totalWatched = uiState.totalWatched,
                        thisMonthCount = uiState.thisMonthCount
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    RecordSections(uiState, onRecordClick, onNavigateToLibraryTab)
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun RecordSections(
    uiState: HomeUiState,
    onRecordClick: (MovieRecord) -> Unit,
    onNavigateToLibraryTab: (WatchStatus) -> Unit,
) {
    Column {
        SectionHeader(
            title = "최근에 본 영화",
            onMoreClick = { onNavigateToLibraryTab(WatchStatus.WATCHED) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (uiState.recentWatched.isEmpty()) {
            EmptyMessage("아직 감상 기록이 없어요. 영화를 검색해 기록해보세요!")
        } else {
            MoviePosterRow(
                records = uiState.recentWatched,
                onRecordClick = onRecordClick
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        SectionHeader(
            title = "보고 싶은 영화",
            onMoreClick = { onNavigateToLibraryTab(WatchStatus.WISHLIST) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (uiState.wishlistPreview.isEmpty()) {
            EmptyMessage("위시리스트가 비어있어요. 보고 싶은 영화를 추가해보세요!")
        } else {
            MoviePosterRow(
                records = uiState.wishlistPreview,
                onRecordClick = onRecordClick
            )
        }
    }
}

@Composable
private fun StatsSummaryPanel(totalWatched: Int, thisMonthCount: Int, onNavigateToCalendar: () -> Unit) {
    Column(
        modifier = Modifier.testTag(HomeTestTags.StatsPanel),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "나의 감상 요약",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        StatCard(modifier = Modifier.fillMaxWidth(), label = "이번 달", value = "${thisMonthCount}편", highlighted = true)
        StatCard(modifier = Modifier.fillMaxWidth(), label = "총 감상", value = "${totalWatched}편")
        OutlinedButton(onClick = onNavigateToCalendar, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("관람 캘린더 보기")
        }
    }
}

@Composable
private fun StatsSummaryRow(totalWatched: Int, thisMonthCount: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = "이번 달",
            value = "${thisMonthCount}편",
            highlighted = true
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = "총 감상",
            value = "${totalWatched}편"
        )
    }
}

/** Brand mark, app name and the calendar shortcut. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeHeader(onNavigateToCalendar: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(TopAppBarDefaults.windowInsets)
            .padding(start = HorizontalPadding, end = HorizontalPadding, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.ic_popcorn_mark),
            contentDescription = null,
            modifier = Modifier.size(34.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.weight(1f)
        )
        OutlinedIconButton(
            onClick = onNavigateToCalendar,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            colors = IconButtonDefaults.outlinedIconButtonColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
            ),
            modifier = Modifier.size(44.dp)
        ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = "캘린더")
        }
    }
}

/** The highlighted card is filled with butter; the other one is a white outlined card. */
@Composable
private fun StatCard(modifier: Modifier = Modifier, label: String, value: String, highlighted: Boolean = false) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = if (highlighted) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun SectionHeader(title: String, onMoreClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        if (onMoreClick != null) {
            TextButton(onClick = onMoreClick) {
                Text("전체 보기", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun EmptyMessage(message: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.large
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
private fun MoviePosterRow(records: List<MovieRecord>, onRecordClick: (MovieRecord) -> Unit = {}) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        // Room for the poster shadow, which LazyRow would otherwise clip
        contentPadding = PaddingValues(end = 8.dp, bottom = 8.dp)
    ) {
        items(records) { record ->
            MoviePosterItem(record = record, onClick = { onRecordClick(record) })
        }
    }
}

@Composable
private fun MoviePosterItem(record: MovieRecord, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .width(PosterWidth)
            .clickable(onClick = onClick)
    ) {
        AsyncImage(
            model = record.posterUrl,
            contentDescription = record.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(PosterWidth, PosterWidth * 1.5f)
                .shadow(6.dp, MaterialTheme.shapes.medium, ambientColor = PopcornColors.Shadow, spotColor = PopcornColors.Shadow)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = record.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        record.rating?.let { rating ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = PopcornColors.Star,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = rating.toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private val PosterWidth = 112.dp

private val HorizontalPadding = 16.dp

internal object HomeTestTags {
    const val TwoColumn = "home_two_column"
    const val StatsPanel = "home_stats"
}

@AdaptiveWindowPreviews
@Composable
private fun HomeContentPreview() {
    val records = (1..5).map {
        MovieRecord(id = "$it", tmdbId = it, title = "영화 $it", status = WatchStatus.WATCHED, rating = 4f)
    }
    AdaptivePreviewSurface {
        HomeContent(
            uiState = HomeUiState(
                recentWatched = records,
                wishlistPreview = records.take(2),
                totalWatched = 42,
                thisMonthCount = 3
            ),
            onRecordClick = {},
            onNavigateToLibraryTab = {},
            onNavigateToCalendar = {}
        )
    }
}
