package com.mymovie.log.presentation.stats

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mymovie.log.presentation.ui.LoginRequiredContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    isLoggedIn: Boolean = true,
    onNavigateToLogin: () -> Unit = {},
    viewModel: StatsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("통계", fontWeight = FontWeight.Bold) })

        if (!isLoggedIn) {
            LoginRequiredContent(
                message = "나의 영화 통계를 보려면\n로그인이 필요해요",
                onNavigateToLogin = onNavigateToLogin
            )
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Overall stats cards
            Row(modifier = Modifier.fillMaxWidth()) {
                StatCard(modifier = Modifier.weight(1f), label = "총 감상", value = "${uiState.totalWatched}편")
                Spacer(modifier = Modifier.padding(4.dp))
                StatCard(modifier = Modifier.weight(1f), label = "평균 평점", value = "★ ${uiState.averageRating}")
                Spacer(modifier = Modifier.padding(4.dp))
                StatCard(modifier = Modifier.weight(1f), label = "위시리스트", value = "${uiState.wishlistCount}편")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("이번 달", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            StatCard(
                modifier = Modifier.fillMaxWidth(),
                label = "감상한 영화",
                value = "${uiState.thisMonthCount}편"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("최근 6개월", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            uiState.monthlyStats.forEach { (label, count) ->
                MonthStatRow(
                    month = label,
                    count = count,
                    maxCount = uiState.monthlyStats.values.maxOrNull() ?: 1
                )
            }
        }
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
            text = "${count}편",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(0.1f)
        )
    }
}
