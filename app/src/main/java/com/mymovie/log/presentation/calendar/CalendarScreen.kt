package com.mymovie.log.presentation.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.mymovie.log.domain.model.MovieRecord
import com.mymovie.log.presentation.ui.RecordDetailBottomSheet
import kotlinx.coroutines.launch
import com.mymovie.log.presentation.ui.LoginRequiredContent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBack: () -> Unit,
    isLoggedIn: Boolean = true,
    onNavigateToLogin: () -> Unit = {},
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val watchedDates by viewModel.watchedDates.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val selectedDateRecords by viewModel.selectedDateRecords.collectAsStateWithLifecycle()
    val selectedRecord by viewModel.selectedRecord.collectAsStateWithLifecycle()
    val editRecordState by viewModel.editRecordState.collectAsStateWithLifecycle()
    val holidayDates by viewModel.holidayDates.collectAsStateWithLifecycle()

    val firstDayOfWeek = firstDayOfWeekFromLocale()
    val maxMonth = YearMonth.now().plusMonths(3)
    val calendarState = rememberCalendarState(
        startMonth = YearMonth.now().minusMonths(12),
        endMonth = maxMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )
    val scope = rememberCoroutineScope()
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("관람 캘린더", fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                }
            }
        )

        if (!isLoggedIn) {
            LoginRequiredContent(
                message = "관람 캘린더를 보려면\n로그인이 필요해요",
                onNavigateToLogin = onNavigateToLogin
            )
            return@Column
        }

        // Month navigation header
        MonthNavigationHeader(
            currentMonth = currentMonth,
            maxMonth = maxMonth,
            onPreviousMonth = {
                val prev = currentMonth.minusMonths(1)
                viewModel.onMonthChange(prev)
                scope.launch { calendarState.animateScrollToMonth(prev) }
            },
            onNextMonth = {
                val next = currentMonth.plusMonths(1)
                if (next <= maxMonth) {
                    viewModel.onMonthChange(next)
                    scope.launch { calendarState.animateScrollToMonth(next) }
                }
            }
        )

        // Day-of-week header aligned with firstDayOfWeek
        DayOfWeekHeader(firstDayOfWeek = firstDayOfWeek)

        // Update current month when the calendar is scrolled
        LaunchedEffect(calendarState.firstVisibleMonth) {
            viewModel.onMonthChange(calendarState.firstVisibleMonth.yearMonth)
        }

        // Calendar
        HorizontalCalendar(
            state = calendarState,
            dayContent = { day ->
                CalendarDayCell(
                    day = day,
                    isWatched = day.date in watchedDates,
                    isSelected = day.date == selectedDate,
                    isHoliday = day.date in holidayDates,
                    onClick = {
                        if (day.position == DayPosition.MonthDate) {
                            viewModel.onDateSelected(day.date)
                        }
                    }
                )
            }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "● 영화를 본 날",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 16.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }

    // Show BottomSheet when a date is selected
    if (selectedDate != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.onBottomSheetDismissed() },
            sheetState = bottomSheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            DateRecordsBottomSheet(
                date = selectedDate!!,
                records = selectedDateRecords,
                onRecordClick = viewModel::selectRecord
            )
        }
    }

    selectedRecord?.let { record ->
        RecordDetailBottomSheet(
            record = record,
            editState = editRecordState,
            onDismiss = viewModel::clearSelectedRecord,
            onSave = viewModel::updateRecord
        )
    }
}

@Composable
private fun MonthNavigationHeader(
    currentMonth: YearMonth,
    maxMonth: YearMonth,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("yyyy년 M월")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPreviousMonth) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "이전 달")
        }
        Text(
            text = currentMonth.format(formatter),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        IconButton(
            onClick = onNextMonth,
            enabled = currentMonth < maxMonth
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "다음 달")
        }
    }
}

@Composable
private fun DayOfWeekHeader(firstDayOfWeek: DayOfWeek) {
    val sundayCol = (DayOfWeek.SUNDAY.value - firstDayOfWeek.value + 7) % 7
    val saturdayCol = (DayOfWeek.SATURDAY.value - firstDayOfWeek.value + 7) % 7
    val days = (0 until 7).map { offset ->
        DayOfWeek.of(((firstDayOfWeek.value - 1 + offset) % 7) + 1)
            .getDisplayName(TextStyle.NARROW, Locale.getDefault())
    }
    Row(modifier = Modifier.fillMaxWidth()) {
        days.forEachIndexed { index, day ->
            Text(
                text = day,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = when (index) {
                    sundayCol -> MaterialTheme.colorScheme.error
                    saturdayCol -> Color(0xFF1976D2)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: CalendarDay,
    isWatched: Boolean,
    isSelected: Boolean,
    isHoliday: Boolean,
    onClick: () -> Unit
) {
    val isCurrentMonth = day.position == DayPosition.MonthDate
    val isToday = day.date == LocalDate.now()
    val dayOfWeek = day.date.dayOfWeek

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isSelected) Modifier.background(MaterialTheme.colorScheme.primaryContainer)
                else Modifier
            )
            .clickable(enabled = isCurrentMonth, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    !isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    isHoliday || dayOfWeek == DayOfWeek.SUNDAY -> MaterialTheme.colorScheme.error
                    dayOfWeek == DayOfWeek.SATURDAY -> Color(0xFF1976D2)
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
            if (isWatched && isCurrentMonth) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
private fun DateRecordsBottomSheet(
    date: LocalDate,
    records: List<MovieRecord>,
    onRecordClick: (MovieRecord) -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("M월 d일 (E)")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = date.format(formatter),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "이 날은 아직 기록이 없어요",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(records) { record ->
                    DateRecordItem(record = record, onClick = { onRecordClick(record) })
                }
            }
        }
    }
}

@Composable
private fun DateRecordItem(record: MovieRecord, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = record.posterUrl,
            contentDescription = record.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp, 75.dp)
                .clip(RoundedCornerShape(6.dp))
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp)
        ) {
            Text(text = record.title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
            record.rating?.let {
                Text(text = "★ $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            record.review?.let {
                Text(text = it, style = MaterialTheme.typography.bodySmall, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
