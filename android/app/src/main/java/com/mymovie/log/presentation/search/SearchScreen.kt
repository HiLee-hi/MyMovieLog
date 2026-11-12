package com.mymovie.log.presentation.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.key
import androidx.compose.ui.platform.testTag
import androidx.paging.compose.LazyPagingItems
import com.mymovie.log.presentation.adaptive.AdaptiveDimens
import com.mymovie.log.presentation.adaptive.LocalAdaptiveLayoutInfo
import com.mymovie.log.presentation.adaptive.paneScaffoldDirective
import androidx.compose.material3.Text
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
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import coil.compose.AsyncImage
import com.mymovie.log.domain.model.Movie

/**
 * Search results with a list-detail layout.
 *
 * One pane (compact / narrow windows): tapping a result navigates to the MovieDetail destination
 * exactly as before. Two panes (the window fits both panes next to the navigation rail, or a
 * separating hinge splits it): the result opens in the detail pane next to the list.
 * If the window shrinks while a movie is open in the detail pane (fold, split screen), the same
 * pane stays on screen — with its record draft — and back returns to the list.
 *
 * @param detailPane hosts the movie detail for [movieId]; `isSinglePane` tells it to show its own
 *   back arrow, `onClose` returns to the results.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun SearchScreen(
    onMovieClick: (Int) -> Unit = {},
    detailPane: @Composable (movieId: Int, isSinglePane: Boolean, onClose: () -> Unit) -> Unit =
        { _, _, _ -> },
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val isSearchActive by viewModel.isSearchActive.collectAsStateWithLifecycle()
    val lazyPagingItems = viewModel.searchResults.collectAsLazyPagingItems()
    // Hoisted so the result position survives the list pane being hidden and shown again
    val listState = rememberLazyListState()

    val adaptiveInfo = LocalAdaptiveLayoutInfo.current
    val paneCount = adaptiveInfo.paneCount(
        listPaneWidth = AdaptiveDimens.ListPaneWidth,
        detailPaneMinWidth = AdaptiveDimens.DetailPaneMinWidth
    )
    // Saves the selected movie id with rememberSaveable, so it survives configuration changes
    val navigator = rememberListDetailPaneScaffoldNavigator<Int>(
        scaffoldDirective = adaptiveInfo.paneScaffoldDirective(paneCount, AdaptiveDimens.ListPaneWidth)
    )
    val selectedMovieId = navigator.currentDestination
        ?.takeIf { it.pane == ListDetailPaneScaffoldRole.Detail }
        ?.content

    BackHandler(enabled = navigator.canNavigateBack()) { navigator.navigateBack() }

    ListDetailPaneScaffold(
        directive = navigator.scaffoldDirective,
        value = navigator.scaffoldValue,
        listPane = {
            AnimatedPane(modifier = Modifier.preferredWidth(AdaptiveDimens.ListPaneWidth)) {
                SearchListPane(
                    query = query,
                    isSearchActive = isSearchActive,
                    lazyPagingItems = lazyPagingItems,
                    listState = listState,
                    selectedMovieId = if (paneCount > 1) selectedMovieId else null,
                    onQueryChange = viewModel::onQueryChange,
                    onActiveChange = viewModel::onActiveChange,
                    onMovieClick = { movie ->
                        if (paneCount > 1) {
                            navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, movie.id)
                        } else {
                            onMovieClick(movie.id)
                        }
                    }
                )
            }
        },
        detailPane = {
            AnimatedPane(modifier = Modifier.testTag(SearchTestTags.DetailPane)) {
                if (selectedMovieId != null) {
                    key(selectedMovieId) {
                        detailPane(selectedMovieId, paneCount == 1) { navigator.navigateBack() }
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "영화를 선택하면 상세 정보가 여기에 표시돼요",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchListPane(
    query: String,
    isSearchActive: Boolean,
    lazyPagingItems: LazyPagingItems<Movie>,
    listState: LazyListState,
    selectedMovieId: Int?,
    onQueryChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onMovieClick: (Movie) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().testTag(SearchTestTags.ListPane)) {
        // The expanded bar covers the whole pane, so the title only shows while collapsed
        if (!isSearchActive) {
            Text(
                text = "검색",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .windowInsetsPadding(SearchBarDefaults.windowInsets)
                    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp)
            )
        }
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = onQueryChange,
                    onSearch = { onActiveChange(false) },
                    expanded = isSearchActive,
                    onExpandedChange = onActiveChange,
                    placeholder = { Text("영화 제목으로 검색") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { onQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "지우기")
                            }
                        }
                    }
                )
            },
            expanded = isSearchActive,
            onExpandedChange = onActiveChange,
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                dividerColor = MaterialTheme.colorScheme.outlineVariant
            ),
            // Collapsed, the title above already keeps clear of the status bar
            windowInsets = if (isSearchActive) SearchBarDefaults.windowInsets else WindowInsets(0),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isSearchActive) 0.dp else 16.dp, vertical = 8.dp)
                .then(
                    if (isSearchActive) Modifier
                    else Modifier.border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primaryContainer,
                        SearchBarDefaults.inputFieldShape
                    )
                )
        ) {
            SearchResultContent(
                query = query,
                lazyPagingItems = lazyPagingItems,
                listState = listState,
                selectedMovieId = selectedMovieId,
                onMovieClick = onMovieClick
            )
        }

        if (!isSearchActive) {
            SearchResultContent(
                query = query,
                lazyPagingItems = lazyPagingItems,
                listState = listState,
                selectedMovieId = selectedMovieId,
                onMovieClick = onMovieClick
            )
        }
    }
}

@Composable
private fun SearchResultContent(
    query: String,
    lazyPagingItems: LazyPagingItems<Movie>,
    listState: LazyListState,
    selectedMovieId: Int?,
    onMovieClick: (Movie) -> Unit
) {
    val refreshState = lazyPagingItems.loadState.refresh

    when {
        query.isBlank() -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("영화를 검색해보세요", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        refreshState is LoadState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        refreshState is LoadState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "오류: ${refreshState.error.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        lazyPagingItems.itemCount == 0 -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("검색 결과가 없어요", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        else -> {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(lazyPagingItems.itemCount) { index ->
                    val movie = lazyPagingItems[index]
                    if (movie != null) {
                        MovieSearchItem(
                            movie = movie,
                            isSelected = movie.id == selectedMovieId,
                            onClick = { onMovieClick(movie) }
                        )
                    }
                }

                if (lazyPagingItems.loadState.append is LoadState.Loading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MovieSearchItem(movie: Movie, onClick: () -> Unit, isSelected: Boolean = false) {
    // A selected card (the movie shown in the detail pane) turns butter
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            AsyncImage(
                model = movie.posterUrl,
                contentDescription = movie.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(64.dp, 96.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = movie.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (movie.originalTitle != movie.title) {
                        Text(
                            text = movie.originalTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    if (movie.releaseDate.length >= 4) {
                        YearChip(movie.releaseDate.take(4))
                    }
                }
                Text(
                    text = movie.overview,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun YearChip(year: String) {
    Text(
        text = year,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

internal object SearchTestTags {
    const val ListPane = "search_list_pane"
    const val DetailPane = "search_detail_pane"
}
