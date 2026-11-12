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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.layout.ThreePaneScaffoldValue
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import com.kizitonwose.calendar.compose.CalendarState
import com.mymovie.log.domain.model.WatchStatus
import com.mymovie.log.presentation.adaptive.LocalAdaptiveLayoutInfo
import com.mymovie.log.presentation.adaptive.constrainedWidth
import com.mymovie.log.presentation.adaptive.paneScaffoldDirective
import com.mymovie.log.presentation.ui.AddRecordState
import com.mymovie.log.presentation.ui.RecordDetailContent
import com.mymovie.log.presentation.ui.RecordDraft
import kotlinx.coroutines.launch
import com.mymovie.log.presentation.ui.LoginRequiredContent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3AdaptiveApi::class)
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
    val editDraft by viewModel.editDraft.collectAsStateWithLifecycle()
    val holidayDates by viewModel.holidayDates.collectAsStateWithLifecycle()

    val firstDayOfWeek = firstDayOfWeekFromLocale()
    val maxMonth = YearMonth.now().plusMonths(3)
    // Hoisted above the adaptive branches so the visible month survives pane changes
    val calendarState = rememberCalendarState(
        startMonth = YearMonth.now().minusMonths(12),
        endMonth = maxMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = firstDayOfWeek
    )
    val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Calendar is shown without the navigation rail, so the whole window width is available.
    // 3 panes only when the window is Large and every pane still gets its minimum width.
    val paneCount = LocalAdaptiveLayoutInfo.current.paneCount(
        listPaneWidth = CalendarPaneWidth,
        detailPaneMinWidth = RecordsPaneMinWidth,
        extraPaneMinWidth = RecordDetailPaneWidth,
        hasNavigationRail = false
    )
    val adaptiveInfo = LocalAdaptiveLayoutInfo.current

    val calendarPane: @Composable (Modifier) -> Unit = { modifier ->
        CalendarPane(
            calendarState = calendarState,
            currentMonth = currentMonth,
            maxMonth = maxMonth,
            firstDayOfWeek = firstDayOfWeek,
            watchedDates = watchedDates,
            holidayDates = holidayDates,
            selectedDate = selectedDate,
            onMonthChange = viewModel::onMonthChange,
            onDateSelected = viewModel::onDateSelected,
            modifier = modifier
        )
    }

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

        if (paneCount == 1) {
            calendarPane(Modifier.constrainedWidth(CalendarSinglePaneMaxWidth))
        } else {
            ListDetailPaneScaffold(
                directive = adaptiveInfo.paneScaffoldDirective(paneCount, CalendarPaneWidth),
                value = ThreePaneScaffoldValue(
                    primary = PaneAdaptedValue.Expanded,
                    secondary = PaneAdaptedValue.Expanded,
                    tertiary = if (paneCount >= 3) PaneAdaptedValue.Expanded else PaneAdaptedValue.Hidden
                ),
                listPane = {
                    AnimatedPane(modifier = Modifier.preferredWidth(CalendarPaneWidth)) {
                        calendarPane(Modifier.testTag(CalendarTestTags.CalendarPane))
                    }
                },
                detailPane = {
                    AnimatedPane {
                        DateRecordsPane(
                            date = selectedDate,
                            records = selectedDateRecords,
                            selectedRecordId = selectedRecord?.id,
                            onRecordClick = viewModel::selectRecord
                        )
                    }
                },
                extraPane = {
                    AnimatedPane(modifier = Modifier.preferredWidth(RecordDetailPaneWidth)) {
                        RecordDetailPane(
                            record = selectedRecord,
                            draft = editDraft,
                            onDraftChange = viewModel::onEditDraftChange,
                            editState = editRecordState,
                            onSaved = viewModel::clearSelectedRecord,
                            onSave = viewModel::updateRecord
                        )
                    }
                }
            )
        }
    }

    // Compact: date records in a BottomSheet (original behavior)
    if (paneCount == 1 && selectedDate != null) {
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

    // Record editor: sheet unless the third pane is showing it
    if (paneCount < 3) {
        selectedRecord?.let { record ->
            RecordDetailBottomSheet(
                record = record,
                draft = editDraft,
                onDraftChange = viewModel::onEditDraftChange,
                editState = editRecordState,
                onDismiss = viewModel::clearSelectedRecord,
                onSave = viewModel::updateRecord
            )
        }
    }
}

@Composable
private fun CalendarPane(
    calendarState: CalendarState,
    currentMonth: YearMonth,
    maxMonth: YearMonth,
    firstDayOfWeek: DayOfWeek,
    watchedDates: Set<LocalDate>,
    holidayDates: Set<LocalDate>,
    selectedDate: LocalDate?,
    onMonthChange: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    // Scrollable so short windows (landscape, split screen, tabletop) never clip the month
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        // Month navigation header
        MonthNavigationHeader(
            currentMonth = currentMonth,
            maxMonth = maxMonth,
            onPreviousMonth = {
                val prev = currentMonth.minusMonths(1)
                onMonthChange(prev)
                scope.launch { calendarState.animateScrollToMonth(prev) }
            },
            onNextMonth = {
                val next = currentMonth.plusMonths(1)
                if (next <= maxMonth) {
                    onMonthChange(next)
                    scope.launch { calendarState.animateScrollToMonth(next) }
                }
            }
        )

        // Day-of-week header aligned with firstDayOfWeek
        DayOfWeekHeader(firstDayOfWeek = firstDayOfWeek)

        // Update current month when the calendar is scrolled
        LaunchedEffect(calendarState.firstVisibleMonth) {
            onMonthChange(calendarState.firstVisibleMonth.yearMonth)
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
                            onDateSelected(day.date)
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
        Spacer(modifier = Modifier.height(16.dp))
    }
}

/** Second pane on wide windows: records of the selected date. */
@Composable
private fun DateRecordsPane(
    date: LocalDate?,
    records: List<MovieRecord>,
    selectedRecordId: String?,
    onRecordClick: (MovieRecord) -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize().testTag(CalendarTestTags.RecordsPane)) {
        if (date == null) {
            PanePlaceholder("날짜를 선택하면\n그날의 기록이 보여요")
        } else {
            DateRecordsBottomSheet(
                date = date,
                records = records,
                selectedRecordId = selectedRecordId,
                onRecordClick = onRecordClick,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

/** Third pane on large windows: the record editor that is a BottomSheet elsewhere. */
@Composable
private fun RecordDetailPane(
    record: MovieRecord?,
    draft: RecordDraft,
    onDraftChange: (RecordDraft) -> Unit,
    editState: AddRecordState,
    onSaved: () -> Unit,
    onSave: (WatchStatus, Float?, LocalDate?, String?, String?) -> Unit,
) {
    LaunchedEffect(editState) {
        if (editState is AddRecordState.Success) onSaved()
    }
    Box(modifier = Modifier.fillMaxSize().testTag(CalendarTestTags.RecordDetailPane)) {
        if (record == null) {
            PanePlaceholder("기록을 선택하면\n여기에서 바로 수정할 수 있어요")
        } else {
            RecordDetailContent(
                record = record,
                draft = draft,
                onDraftChange = onDraftChange,
                editState = editState,
                onSave = onSave,
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun PanePlaceholder(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
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
    onRecordClick: (MovieRecord) -> Unit,
    modifier: Modifier = Modifier,
    selectedRecordId: String? = null,
) {
    val formatter = DateTimeFormatter.ofPattern("M월 d일 (E)")
    Column(
        modifier = modifier
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
                    DateRecordItem(
                        record = record,
                        isSelected = record.id == selectedRecordId,
                        onClick = { onRecordClick(record) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateRecordItem(record: MovieRecord, onClick: () -> Unit, isSelected: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isSelected) Modifier.background(MaterialTheme.colorScheme.secondaryContainer)
                else Modifier
            )
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

/** Calendar pane: 48dp day cells. */
private val CalendarPaneWidth: Dp = 336.dp
private val RecordsPaneMinWidth: Dp = 300.dp
private val RecordDetailPaneWidth: Dp = 400.dp

/** Single-pane calendar on a medium window stays compact instead of growing huge cells. */
private val CalendarSinglePaneMaxWidth: Dp = 560.dp

internal object CalendarTestTags {
    const val CalendarPane = "calendar_pane"
    const val RecordsPane = "calendar_records_pane"
    const val RecordDetailPane = "calendar_record_detail_pane"
}
