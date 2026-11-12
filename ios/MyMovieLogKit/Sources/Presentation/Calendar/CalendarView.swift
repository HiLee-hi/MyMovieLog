import Domain
import SwiftUI

struct CalendarView: View {
    private let isLoggedIn: Bool
    private let onNavigateToLogin: () -> Void
    @State private var viewModel: CalendarViewModel
    @State private var width: CGFloat = 0

    private static let weekdays = ["일", "월", "화", "수", "목", "금", "토"]

    init(useCases: UseCaseContainer, isLoggedIn: Bool, onNavigateToLogin: @escaping () -> Void) {
        self.isLoggedIn = isLoggedIn
        self.onNavigateToLogin = onNavigateToLogin
        _viewModel = State(initialValue: CalendarViewModel(
            getWatchedDatesByMonth: useCases.getWatchedDatesByMonth,
            getRecordsByDate: useCases.getRecordsByDate,
            upsertRecord: useCases.upsertRecord
        ))
    }

    var body: some View {
        Group {
            if isLoggedIn {
                content
            } else {
                LoginRequiredView(onNavigateToLogin: onNavigateToLogin)
            }
        }
        .navigationTitle("관람 캘린더")
        .inlineNavigationTitle()
        .onWidthChange { width = $0 }
    }

    /// Unfolded or wide windows show the day's records beside the month instead of in a sheet
    private var isTwoPane: Bool { AdaptiveDimens.fitsListDetail(width) }

    @ViewBuilder
    private var content: some View {
        if isTwoPane {
            HStack(alignment: .top, spacing: AdaptiveDimens.paneSpacing) {
                ScrollView {
                    monthCalendar
                        .padding(14)
                        .popcornCard()
                        .padding(.vertical)
                }
                .frame(width: 380)
                DateRecordsPane(viewModel: viewModel)
                    .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
            }
            .padding(.horizontal, 28)
            .frame(maxWidth: AdaptiveDimens.contentMaxWidth)
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(Color.popcornCream)
            .modifier(MonthControls(viewModel: viewModel))
        } else {
            ScrollView {
                monthCalendar
                    .padding()
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
            }
            .modifier(MonthControls(viewModel: viewModel))
            .sheet(isPresented: Binding(
                get: { viewModel.selectedDate != nil },
                // Unfolding closes the sheet but keeps the date, which the side pane then shows
                set: { if !$0 && !isTwoPane { viewModel.dismissDate() } }
            )) {
                DateRecordsSheet(viewModel: viewModel)
            }
        }
    }

    private var monthCalendar: some View {
        VStack(spacing: 16) {
            monthHeader
            weekdayHeader
            monthGrid
            HStack(spacing: 6) {
                Circle().fill(Color.accentColor).frame(width: 6, height: 6)
                Text("영화를 본 날")
            }
            .font(.caption)
            .foregroundStyle(.secondary)
            .frame(maxWidth: .infinity, alignment: .trailing)
        }
    }

    private var monthHeader: some View {
        HStack {
            Button {
                withAnimation { viewModel.showPreviousMonth() }
            } label: {
                Image(systemName: "chevron.left")
            }
            .accessibilityLabel("이전 달")
            Spacer()
            Text(viewModel.currentMonth.koreanText)
                .font(.title3.bold())
                .contentTransition(.numericText())
            Spacer()
            Button {
                withAnimation { viewModel.showNextMonth() }
            } label: {
                Image(systemName: "chevron.right")
            }
            .accessibilityLabel("다음 달")
        }
        .buttonStyle(.borderless)
        .font(.title3)
    }

    private var weekdayHeader: some View {
        HStack(spacing: 0) {
            ForEach(Array(Self.weekdays.enumerated()), id: \.offset) { index, name in
                Text(name)
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(weekdayColor(column: index))
                    .frame(maxWidth: .infinity)
            }
        }
    }

    private var monthGrid: some View {
        let month = viewModel.currentMonth
        let leadingBlanks = month.firstWeekday - 1
        let cells: [Int?] = Array(repeating: nil, count: leadingBlanks) + (1...month.numberOfDays).map { $0 }
        let today = LocalDate.today()
        return LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 0), count: 7), spacing: 8) {
            ForEach(Array(cells.enumerated()), id: \.offset) { index, day in
                if let day {
                    let date = month.day(day)
                    DayCell(
                        day: day,
                        isToday: date == today,
                        isWatched: viewModel.watchedDates.contains(date),
                        // Only the side pane keeps a date selected; the sheet covers the grid
                        isSelected: isTwoPane && viewModel.selectedDate == date,
                        color: weekdayColor(column: index % 7)
                    ) {
                        viewModel.selectDate(date)
                    }
                } else {
                    Color.clear.frame(height: 44)
                }
            }
        }
    }

    private func weekdayColor(column: Int) -> Color {
        switch column {
        case 0: return .red
        case 6: return .blue
        default: return .primary
        }
    }
}

/// Swipe between months, the "today" button and the watched-day dots, shared by both layouts
private struct MonthControls: ViewModifier {
    let viewModel: CalendarViewModel

    func body(content: Content) -> some View {
        content
            .gesture(
                DragGesture(minimumDistance: 30).onEnded { value in
                    guard abs(value.translation.width) > abs(value.translation.height) else { return }
                    withAnimation {
                        if value.translation.width < 0 { viewModel.showNextMonth() } else { viewModel.showPreviousMonth() }
                    }
                }
            )
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button("오늘") { withAnimation { viewModel.showToday() } }
                }
            }
            .task(id: viewModel.currentMonth) { await viewModel.observeWatchedDates() }
    }
}

private struct DayCell: View {
    let day: Int
    let isToday: Bool
    let isWatched: Bool
    let isSelected: Bool
    let color: Color
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 3) {
                Text("\(day)")
                    .font(.body.weight(isToday || isSelected ? .bold : .regular))
                    .foregroundStyle(isSelected ? Color.popcornInk : color)
                    .frame(width: 34, height: 34)
                    .background {
                        if isSelected {
                            Circle().fill(Color.popcornButter)
                        } else if isToday {
                            Circle().fill(Color.accentColor.opacity(0.18))
                        }
                    }
                Circle()
                    .fill(isWatched && !isSelected ? Color.accentColor : Color.clear)
                    .frame(width: 6, height: 6)
            }
            .frame(maxWidth: .infinity, minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(day)일\(isWatched ? ", 영화 본 날" : "")")
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

/// Records watched on the selected day, like the Android date BottomSheet
private struct DateRecordsSheet: View {
    @Bindable var viewModel: CalendarViewModel

    var body: some View {
        NavigationStack {
            List {
                if viewModel.selectedDateRecords.isEmpty {
                    Text("이 날은 아직 기록이 없어요")
                        .foregroundStyle(.secondary)
                } else {
                    ForEach(viewModel.selectedDateRecords) { record in
                        Button {
                            viewModel.editSession.select(record)
                        } label: {
                            DateRecordRow(record: record)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .navigationTitle(viewModel.selectedDate?.koreanText ?? "")
            .inlineNavigationTitle()
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button("닫기") { viewModel.dismissDate() }
                }
            }
            .task(id: viewModel.selectedDate) { await viewModel.observeSelectedDateRecords() }
            .sheet(isPresented: viewModel.editSession.isPresentedBinding) {
                viewModel.editSession.editorSheet()
            }
        }
        .presentationDetents([.medium, .large])
    }
}

/// Side pane of the two-pane calendar: the selected day's records, tapped to edit in place
private struct DateRecordsPane: View {
    @Bindable var viewModel: CalendarViewModel

    var body: some View {
        Group {
            if let date = viewModel.selectedDate {
                ScrollView {
                    VStack(alignment: .leading, spacing: 10) {
                        Text(date.koreanText)
                            .font(.headline)
                            .foregroundStyle(Color.popcornInk)
                            .accessibilityAddTraits(.isHeader)
                        if viewModel.selectedDateRecords.isEmpty {
                            Text("이 날은 아직 기록이 없어요")
                                .font(.subheadline)
                                .foregroundStyle(Color.popcornInkMuted)
                                .frame(maxWidth: .infinity, minHeight: 80)
                                .popcornCard(fill: .popcornButterLight, outlined: false)
                        } else {
                            ForEach(viewModel.selectedDateRecords) { record in
                                Button {
                                    viewModel.editSession.select(record)
                                } label: {
                                    DateRecordRow(record: record)
                                        .frame(maxWidth: .infinity, alignment: .leading)
                                        .padding(10)
                                        .popcornCard()
                                }
                                .buttonStyle(.plain)
                            }
                            Text("기록을 누르면 바로 수정할 수 있어요")
                                .font(.footnote)
                                .foregroundStyle(Color.popcornInkMuted)
                        }
                    }
                    .padding(.vertical)
                }
            } else {
                ContentUnavailableView(
                    "날짜를 골라 보세요",
                    systemImage: "calendar",
                    description: Text("날짜를 누르면 그날 본 영화가 여기에 보여요")
                )
            }
        }
        .task(id: viewModel.selectedDate) { await viewModel.observeSelectedDateRecords() }
        .sheet(isPresented: viewModel.editSession.isPresentedBinding) {
            viewModel.editSession.editorSheet()
        }
    }
}

private struct DateRecordRow: View {
    let record: MovieRecord

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            PosterImage(url: record.posterURL, cornerRadius: 6)
                .frame(width: 52)
            VStack(alignment: .leading, spacing: 4) {
                Text(record.title).font(.body.weight(.semibold))
                RatingLabel(rating: record.rating)
                if let review = record.review {
                    Text(review)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
            }
        }
    }
}
