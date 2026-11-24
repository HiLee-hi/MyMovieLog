package com.mymovie.log.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.net.Uri
import coil.compose.AsyncImage
import com.mymovie.log.domain.model.Movie
import com.mymovie.log.presentation.ui.AddRecordBottomSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.LaunchedEffect
import com.mymovie.log.R
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.presentation.adaptive.AdaptiveDimens
import com.mymovie.log.presentation.adaptive.AdaptivePreviewSurface
import com.mymovie.log.presentation.adaptive.AdaptiveTwoColumn
import com.mymovie.log.presentation.adaptive.AdaptiveWindowPreviews
import com.mymovie.log.presentation.adaptive.LocalAdaptiveLayoutInfo
import com.mymovie.log.presentation.adaptive.WindowHeightClass
import com.mymovie.log.presentation.adaptive.constrainedWidth
import com.mymovie.log.presentation.ui.AddRecordState
import com.mymovie.log.presentation.ui.RecordDraft
import com.mymovie.log.presentation.ui.RecordEditorForm
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.mymovie.log.presentation.ui.rememberDateTimeFormatter
import com.mymovie.log.presentation.ui.theme.PopcornColors
import com.mymovie.log.presentation.ui.theme.popcornButtonColors
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailScreen(
    onBack: (recordSaved: Boolean) -> Unit,
    isLoggedIn: Boolean = true,
    onNavigateToLogin: () -> Unit = {},
    onOpenCamera: () -> Unit = {},
    onOpenAlbumPicker: (List<Uri>) -> Unit = {},
    // in a list-detail pane: no back arrow, scaffold owns system back
    showNavigationIcon: Boolean = true,
    handleSystemBack: Boolean = true,
    viewModel: MovieDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showBottomSheet by viewModel.showBottomSheet.collectAsStateWithLifecycle()
    val addRecordState by viewModel.addRecordState.collectAsStateWithLifecycle()
    val existingRecord by viewModel.existingRecord.collectAsStateWithLifecycle()
    val recordDraft by viewModel.recordDraft.collectAsStateWithLifecycle()
    val recordSaved by viewModel.recordSavedThisSession.collectAsStateWithLifecycle()
    val attachedUris by viewModel.attachedUris.collectAsStateWithLifecycle()
    val existingPhotoSignedUrls by viewModel.existingPhotoSignedUrls.collectAsStateWithLifecycle()
    val adaptiveInfo = LocalAdaptiveLayoutInfo.current

    BackHandler(enabled = handleSystemBack) { onBack(recordSaved) }

    Scaffold(
        topBar = {
            if (showNavigationIcon) {
                TopAppBar(
                    title = {},
                    navigationIcon = {
                        FilledIconButton(
                            onClick = { onBack(recordSaved) },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .shadow(6.dp, CircleShape, ambientColor = PopcornColors.Shadow, spotColor = PopcornColors.Shadow)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_navigate_back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        }
    ) { innerPadding ->
        when (val state = uiState) {
            is DetailUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is DetailUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.detail_load_failed),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = viewModel::loadDetail) {
                            Text(stringResource(R.string.common_retry))
                        }
                    }
                }
            }

            is DetailUiState.Success -> {
                val onRecordClick = {
                    if (isLoggedIn) viewModel.onRecordClick() else onNavigateToLogin()
                }
                val onSave: (WatchStatus, Float?, LocalDate?, String?, String?) -> Unit =
                    { status, rating, watchedAt, review, memo ->
                        viewModel.saveRecord(state.movie, status, rating, watchedAt, review, memo)
                    }

                // from this screen's width (window or pane), so a narrow pane keeps the phone layout
                BoxWithConstraints(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                    val isWide = AdaptiveDimens.fitsTwoColumns(maxWidth)
                    val isShortWindow = adaptiveInfo.heightClass == WindowHeightClass.Compact
                    // backdrop under the transparent top bar, otherwise start below the bar
                    val showsBackdrop = !isWide || !isShortWindow

                    // inline editor has no sheet to close after save
                    LaunchedEffect(addRecordState, isWide) {
                        if (isWide && addRecordState is AddRecordState.Success) viewModel.onDismissSheet()
                    }

                    MovieDetailContent(
                        movie = state.movie,
                        record = existingRecord,
                        isEditing = showBottomSheet,
                        isWide = isWide,
                        isShortWindow = isShortWindow,
                        modifier = Modifier.padding(top = if (showsBackdrop) 0.dp else innerPadding.calculateTopPadding()),
                        onRecordClick = onRecordClick,
                        myLogPane = {
                            MyMovieLogPane(
                                record = existingRecord,
                                isEditing = showBottomSheet,
                                draft = recordDraft,
                                onDraftChange = viewModel::onDraftChange,
                                saveState = addRecordState,
                                attachedUris = attachedUris,
                                existingPhotoSignedUrls = existingPhotoSignedUrls,
                                onOpenCamera = onOpenCamera,
                                onOpenAlbumPicker = { onOpenAlbumPicker(attachedUris) },
                                onRemovePhoto = viewModel::removePhoto,
                                onRemoveExistingPhoto = viewModel::removeExistingPhoto,
                                onStartEditing = onRecordClick,
                                onCancel = viewModel::onDismissSheet,
                                onSave = onSave
                            )
                        }
                    )

                    // same editor state as a sheet when there's no room inline
                    if (showBottomSheet && !isWide) {
                        AddRecordBottomSheet(
                            movie = state.movie,
                            draft = recordDraft,
                            onDraftChange = viewModel::onDraftChange,
                            existingPhotoSignedUrls = existingPhotoSignedUrls,
                            addRecordState = addRecordState,
                            attachedUris = attachedUris,
                            onOpenCamera = onOpenCamera,
                            onOpenAlbumPicker = { onOpenAlbumPicker(attachedUris) },
                            onRemovePhoto = viewModel::removePhoto,
                            onRemoveExistingPhoto = viewModel::removeExistingPhoto,
                            onDismiss = viewModel::onDismissSheet,
                            onSave = onSave
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun MovieDetailContent(
    movie: Movie,
    modifier: Modifier = Modifier,
    record: MovieRecord? = null,
    isEditing: Boolean = false,
    isWide: Boolean = false,
    isShortWindow: Boolean = false,
    onRecordClick: () -> Unit,
    myLogPane: @Composable () -> Unit = {},
) {
    if (isWide) {
        WideMovieDetailContent(
            movie = movie,
            isShortWindow = isShortWindow,
            modifier = modifier,
            myLogPane = myLogPane
        )
        return
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            // backdrop, poster overlaps its bottom edge
            Box(modifier = Modifier.fillMaxWidth()) {
                Backdrop(movie = movie, height = BackdropHeight)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = BackdropHeight - PosterOverlap),
                    verticalAlignment = Alignment.Top
                ) {
                    AsyncImage(
                        model = movie.posterUrl,
                        contentDescription = movie.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(112.dp, 168.dp)
                            .shadow(8.dp, MaterialTheme.shapes.medium, ambientColor = PopcornColors.Shadow, spotColor = PopcornColors.Shadow)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .border(3.dp, MaterialTheme.colorScheme.background, MaterialTheme.shapes.medium)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    // below the backdrop so the title stays off the image
                    MovieHeaderInfo(
                        movie = movie,
                        showGenres = false,
                        modifier = Modifier
                            .weight(1f)
                            .padding(top = PosterOverlap + 8.dp)
                    )
                }
            }

            if (movie.genres.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    movie.genres.forEach { GenreChip(it) }
                }
            }

            if (movie.overview.isNotBlank()) {
                OverviewCard(
                    overview = movie.overview,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)
                )
            }

            // hidden while editing, the sheet shows it
            if (record != null && !isEditing) {
                RecordCard(
                    record = record,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        Button(
            colors = popcornButtonColors(),
            onClick = onRecordClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .height(52.dp)
        ) {
            Text(
                if (record != null) stringResource(R.string.record_edit) else stringResource(R.string.record_add),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun Backdrop(movie: Movie, height: Dp) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        AsyncImage(
            model = movie.backdropUrl ?: movie.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        val background = MaterialTheme.colorScheme.background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, background)))
        )
    }
}

@Composable
private fun GenreChip(genre: String) {
    Text(
        text = genre,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun OverviewCard(overview: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.detail_overview),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = overview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecordCard(record: MovieRecord, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth().testTag(MovieDetailTestTags.RecordCard),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.detail_my_record),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            RecordSummary(record)
        }
    }
}

// wide: poster + info + overview | my log (inline editor)
// editor at the top of column 2, visible without scrolling on short 4:3 / landscape
@Composable
private fun WideMovieDetailContent(
    movie: Movie,
    isShortWindow: Boolean,
    modifier: Modifier = Modifier,
    myLogPane: @Composable () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
    ) {
        // short windows (landscape, tabletop, split) skip the backdrop
        if (!isShortWindow) {
            Backdrop(movie = movie, height = 160.dp)
        }

        AdaptiveTwoColumn(
            modifier = Modifier
                .constrainedWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            firstFraction = 0.55f,
            first = {
                Column {
                    Row(verticalAlignment = Alignment.Top) {
                        AsyncImage(
                            model = movie.posterUrl,
                            contentDescription = movie.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(if (isShortWindow) 96.dp else 120.dp)
                                .aspectRatio(POSTER_ASPECT_RATIO)
                                .shadow(8.dp, MaterialTheme.shapes.medium, ambientColor = PopcornColors.Shadow, spotColor = PopcornColors.Shadow)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        MovieHeaderInfo(movie = movie, modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    OverviewCard(
                        overview = movie.overview.ifBlank { stringResource(R.string.detail_overview_empty) },
                        modifier = Modifier.widthIn(max = AdaptiveDimens.ReadableMaxWidth)
                    )
                }
            },
            second = myLogPane
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MovieHeaderInfo(movie: Movie, modifier: Modifier = Modifier, showGenres: Boolean = true) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = movie.title, style = MaterialTheme.typography.headlineSmall)
        val meta = listOfNotNull(
            movie.originalTitle.takeIf { it != movie.title && it.isNotBlank() },
            movie.releaseDate.take(4).takeIf { movie.releaseDate.length >= 4 }
        ).joinToString(" · ")
        if (meta.isNotEmpty()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (movie.voteAverage > 0.0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = PopcornColors.Star,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "%.1f".format(movie.voteAverage),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "TMDB",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (showGenres && movie.genres.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            // wrap, not scroll: room for several rows next to the poster
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                movie.genres.forEach { GenreChip(it) }
            }
        }
    }
}

// "my movie log" column: summary or inline editor
@Composable
private fun MyMovieLogPane(
    record: MovieRecord?,
    isEditing: Boolean,
    draft: RecordDraft,
    onDraftChange: (RecordDraft) -> Unit,
    saveState: AddRecordState,
    attachedUris: List<Uri>,
    existingPhotoSignedUrls: List<String>,
    onOpenCamera: () -> Unit,
    onOpenAlbumPicker: () -> Unit,
    onRemovePhoto: (Uri) -> Unit,
    onRemoveExistingPhoto: (String) -> Unit,
    onStartEditing: () -> Unit,
    onCancel: () -> Unit,
    onSave: (WatchStatus, Float?, LocalDate?, String?, String?) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().testTag(MovieDetailTestTags.MyLogPane),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.detail_my_record),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                if (isEditing) {
                    TextButton(onClick = onCancel, enabled = saveState !is AddRecordState.Saving) {
                        Text(stringResource(R.string.common_cancel))
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            when {
                isEditing -> RecordEditorForm(
                    draft = draft,
                    onDraftChange = onDraftChange,
                    saveState = saveState,
                    saveLabel = stringResource(R.string.record_save),
                    onSave = onSave,
                    attachedUris = attachedUris,
                    existingPhotoUrls = existingPhotoSignedUrls,
                    onOpenCamera = onOpenCamera,
                    onOpenAlbumPicker = onOpenAlbumPicker,
                    onRemovePhoto = onRemovePhoto,
                    onRemoveExistingPhoto = onRemoveExistingPhoto
                )

                record != null -> {
                    RecordSummary(record)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(colors = popcornButtonColors(), onClick = onStartEditing, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.record_edit))
                    }
                }

                else -> {
                    Text(
                        text = stringResource(R.string.detail_no_record),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(colors = popcornButtonColors(), onClick = onStartEditing, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.record_add))
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordSummary(record: MovieRecord) {
    val dateFormatter = rememberDateTimeFormatter(R.string.format_date_full)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = if (record.status == WatchStatus.WATCHED) stringResource(R.string.record_status_watched) else stringResource(R.string.record_status_wishlist),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        record.rating?.let { RatingStars(it) }
        record.watchedAt?.let {
            Text(
                text = it.format(dateFormatter),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        (record.review ?: record.memo)?.takeIf { it.isNotBlank() }?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium)
        }
        if (record.photoUrls.isNotEmpty()) {
            Text(
                text = stringResource(R.string.record_photo_count, record.photoUrls.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// read-only, StarRatingRow is the editable one
@Composable
private fun RatingStars(rating: Float) {
    val ratingDescription = stringResource(R.string.record_rating_summary, rating)
    Row(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = ratingDescription },
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        for (star in 1..5) {
            Icon(
                imageVector = if (rating >= star - 0.5f && rating < star) Icons.AutoMirrored.Filled.StarHalf else Icons.Default.Star,
                contentDescription = null,
                tint = if (rating >= star - 0.5f) PopcornColors.Star else PopcornColors.StarEmpty,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private const val POSTER_ASPECT_RATIO = 2f / 3f
private val BackdropHeight = 240.dp
private val PosterOverlap = 72.dp

internal object MovieDetailTestTags {
    const val MyLogPane = "detail_my_log_pane"
    const val RecordCard = "detail_record_card"
}

internal val PreviewMovie = Movie(
    id = 1,
    title = "미리보기 영화",
    originalTitle = "Preview Movie",
    overview = "적당한 길이의 줄거리 텍스트입니다. ".repeat(12),
    posterPath = null,
    backdropPath = null,
    releaseDate = "2024-05-01",
    voteAverage = 7.8,
    genreIds = emptyList(),
    genres = listOf("드라마", "SF", "모험")
)

@AdaptiveWindowPreviews
@Composable
private fun MovieDetailContentPreview() {
    AdaptivePreviewSurface {
        BoxWithConstraints {
            val info = LocalAdaptiveLayoutInfo.current
            MovieDetailContent(
                movie = PreviewMovie,
                isWide = AdaptiveDimens.fitsTwoColumns(maxWidth),
                isShortWindow = info.heightClass == WindowHeightClass.Compact,
                onRecordClick = {},
                myLogPane = {
                    MyMovieLogPane(
                        record = null, isEditing = true, draft = RecordDraft(review = "리뷰 작성 중"),
                        onDraftChange = {}, saveState = AddRecordState.Idle, attachedUris = emptyList(),
                        existingPhotoSignedUrls = emptyList(), onOpenCamera = {}, onOpenAlbumPicker = {},
                        onRemovePhoto = {}, onRemoveExistingPhoto = {}, onStartEditing = {}, onCancel = {},
                        onSave = { _, _, _, _, _ -> }
                    )
                }
            )
        }
    }
}
