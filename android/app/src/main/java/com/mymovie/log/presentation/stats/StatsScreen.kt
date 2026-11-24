package com.mymovie.log.presentation.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.mymovie.log.presentation.ui.theme.PopcornColors
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mymovie.log.R
import com.mymovie.log.presentation.ui.LoginRequiredContent
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.platform.testTag
import com.mymovie.log.presentation.adaptive.AdaptiveDimens
import com.mymovie.log.presentation.adaptive.AdaptivePreviewSurface
import com.mymovie.log.presentation.adaptive.AdaptiveTwoColumn
import com.mymovie.log.presentation.adaptive.AdaptiveWindowPreviews
import com.mymovie.log.presentation.adaptive.constrainedWidth
import com.mymovie.log.presentation.ui.rememberDateTimeFormatter
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    isLoggedIn: Boolean = true,
    onNavigateToLogin: () -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text(stringResource(R.string.nav_stats), fontWeight = FontWeight.Bold) })

        if (!isLoggedIn) {
            LoginRequiredContent(
                message = stringResource(R.string.stats_login_required),
                onNavigateToLogin = onNavigateToLogin
            )
            return@Column
        }

        StatsContent(uiState = uiState)
    }
}

/**
 * Compact: the original single scrolling column.
 * Wide: summary cards | monthly chart side by side, capped at a readable content width.
 */
@Composable
internal fun StatsContent(uiState: StatsUiState, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isWide = AdaptiveDimens.fitsTwoColumns(maxWidth - 32.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .constrainedWidth()
                    .padding(16.dp)
            ) {
                if (isWide) {
                    AdaptiveTwoColumn(
                        modifier = Modifier.testTag(StatsTestTags.TwoColumn),
                        first = { SummarySection(uiState) },
                        second = { MonthlySection(uiState) }
                    )
                } else {
                    SummarySection(uiState)
                    Spacer(modifier = Modifier.height(24.dp))
                    MonthlySection(uiState)
                }
            }
        }
    }
}

@Composable
private fun SummarySection(uiState: StatsUiState) {
    Column {
        // Overall stats cards
        Row(modifier = Modifier.fillMaxWidth()) {
            StatCard(modifier = Modifier.weight(1f), label = stringResource(R.string.common_total_watched), value = stringResource(R.string.common_movie_count, uiState.totalWatched))
            Spacer(modifier = Modifier.padding(4.dp))
            StatCard(modifier = Modifier.weight(1f), label = stringResource(R.string.stats_average_rating), value = stringResource(R.string.stats_average_rating_value, uiState.averageRating))
            Spacer(modifier = Modifier.padding(4.dp))
            StatCard(modifier = Modifier.weight(1f), label = stringResource(R.string.common_wishlist), value = stringResource(R.string.common_movie_count, uiState.wishlistCount))
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(stringResource(R.string.common_this_month), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        ThisMonthCard(thisMonthCount = uiState.thisMonthCount, lastMonthCount = uiState.lastMonthCount)
    }
}

@Composable
private fun MonthlySection(uiState: StatsUiState) {
    Column {
        Text(stringResource(R.string.stats_recent_six_months), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        MonthlyChart(uiState.monthlyStats)
    }
}

private val MonthBarMaxHeight = 120.dp

/** Vertical bars for the last six months with the current month in butter, as in the design canvas */
@Composable
private fun MonthlyChart(monthlyStats: Map<YearMonth, Int>) {
    val monthFormatter = rememberDateTimeFormatter(R.string.format_month)
    val maxCount = monthlyStats.values.maxOrNull()?.takeIf { it > 0 } ?: 1
    val months = monthlyStats.entries.map { (month, count) -> month.format(monthFormatter) to count }

    Surface(
        modifier = Modifier.fillMaxWidth().testTag(StatsTestTags.MonthlyChart),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().height(MonthBarMaxHeight + 24.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                months.forEachIndexed { index, (month, count) ->
                    MonthBar(
                        month = month,
                        count = count,
                        maxCount = maxCount,
                        isCurrent = index == months.lastIndex,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = PopcornColors.Outline)
            Row(
                modifier = Modifier.fillMaxWidth().clearAndSetSemantics { },
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                months.forEachIndexed { index, (month, _) ->
                    val isCurrent = index == months.lastIndex
                    Text(
                        text = month,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCurrent) PopcornColors.Ink else PopcornColors.InkMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/** The count over a bar whose height is relative to the busiest month */
@Composable
private fun MonthBar(month: String, count: Int, maxCount: Int, isCurrent: Boolean, modifier: Modifier = Modifier) {
    val countText = stringResource(R.string.common_movie_count, count)
    // A month with no records keeps a thin sliver so the column is still visible
    val barHeight = if (count == 0) 3.dp else maxOf(MonthBarMaxHeight * (count.toFloat() / maxCount), 8.dp)
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = "$month $countText" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = countText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
            color = if (isCurrent) PopcornColors.Ink else PopcornColors.InkMuted,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .widthIn(max = 34.dp)
                .fillMaxWidth()
                .height(barHeight)
                .background(
                    color = if (isCurrent) PopcornColors.Butter else PopcornColors.ButterSoft,
                    shape = RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                )
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
            Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ThisMonthCard(thisMonthCount: Int, lastMonthCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(PopcornColors.ButterLight)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.stats_watched_movies),
                style = MaterialTheme.typography.labelLarge,
                color = PopcornColors.InkMuted
            )
            Text(
                text = stringResource(R.string.common_movie_count, thisMonthCount),
                style = MaterialTheme.typography.headlineMedium
            )
        }
        MonthComparison(
            thisMonthCount = thisMonthCount,
            lastMonthCount = lastMonthCount,
            modifier = Modifier.testTag(StatsTestTags.MonthComparison)
        )
    }
}

/** "지난달보다 3편 더": hidden while both months are empty, e.g. right after signing up. */
@Composable
private fun MonthComparison(thisMonthCount: Int, lastMonthCount: Int, modifier: Modifier = Modifier) {
    if (thisMonthCount == 0 && lastMonthCount == 0) return
    val diff = thisMonthCount - lastMonthCount
    val emphasis = SpanStyle(color = PopcornColors.Caramel, fontWeight = FontWeight.Bold)
    val text = if (diff == 0) {
        buildAnnotatedString { append(stringResource(R.string.stats_same_as_last_month)) }
    } else {
        val prefix = stringResource(R.string.stats_vs_last_month)
        val amount = if (diff > 0) stringResource(R.string.stats_vs_last_month_more, diff)
        else stringResource(R.string.stats_vs_last_month_fewer, -diff)
        buildAnnotatedString {
            append(prefix)
            append('\n')
            withStyle(emphasis) { append(amount) }
        }
    }
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = PopcornColors.InkMuted,
        textAlign = TextAlign.End,
        modifier = modifier.padding(start = 12.dp)
    )
}

@Composable
private fun MonthStatRow(month: String, count: Int, maxCount: Int) {
    val barFraction = if (maxCount > 0) count.toFloat() / maxCount else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = month,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(end = 8.dp)
                .weight(0.15f)
        )
        Box(modifier = Modifier.weight(0.75f).height(20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(barFraction)
                    .height(20.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }
        Text(
            text = stringResource(R.string.common_movie_count, count),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(0.1f)
        )
    }
}

internal object StatsTestTags {
    const val TwoColumn = "stats_two_column"
    const val MonthComparison = "stats_month_comparison"
    const val MonthlyChart = "stats_monthly_chart"
}

@AdaptiveWindowPreviews
@Composable
private fun StatsContentPreview() {
    AdaptivePreviewSurface {
        StatsContent(
            uiState = StatsUiState(
                totalWatched = 42,
                wishlistCount = 7,
                averageRating = "4.1",
                thisMonthCount = 3,
                lastMonthCount = 6,
                monthlyStats = (4..9).zip(listOf(2, 5, 1, 4, 6, 3)).associate { (month, count) -> YearMonth.of(2026, month) to count }
            )
        )
    }
}
