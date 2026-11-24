import Domain
import SwiftUI

struct CalendarView: View {
    private let isLoggedIn: Bool
    private let onNavigateToLogin: () -> Void
    @State private var viewModel: CalendarViewModel
    @State private var width: CGFloat = 0

    private static var weekdays: [String] { L10n.weekdays }

    init(useCases: UseCaseContainer, isLoggedIn: Bool, onNavigateToLogin: @escaping () -> Void) {
        self.isLoggedIn = isLoggedIn
        self.onNavigateToLogin = onNavigateToLogin
        _viewModel = State(initialValue: CalendarViewModel(
            getWatchedDatesByMonth: useCases.getWatchedDatesByMonth,
            getRecordsByDate: useCases.getRecordsByDate,
            getHolidaysByMonth: useCases.getHolidaysByMonth,
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
        .navigationTitle(L10n.calendarTitle)
        .inlineNavigationTitle()
        .onWidthChange { width = $0 }
    }

    // wide: day records beside the month, not a sheet
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
                    .padding(.horizontal, 12)
                    .padding(.top, 16)
                    .padding(.bottom, 12)
                    .popcornCard()
                    .padding()
                    .frame(maxWidth: 560)
                    .frame(maxWidth: .infinity)
            }
            .popcornScreenBackground()
            .modifier(MonthControls(viewModel: viewModel))
            .sheet(isPresented: Binding(
                get: { viewModel.selectedDate != nil },
                // unfold closes the sheet, date kept for the side pane
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
                Circle().fill(Color.popcornStar).frame(width: 6, height: 6)
                Text(L10n.calendarLegendWatched)
            }
            .font(.caption)
            .foregroundStyle(Color.popcornInkMuted)
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 4)
        }
    }

    private var monthHeader: some View {
        HStack {
            monthArrow("chevron.left", label: L10n.calendarPreviousMonth) { viewModel.showPreviousMonth() }
            Spacer()
            Text(viewModel.currentMonth.displayText)
                .font(.jua(20, relativeTo: .title3))
                .foregroundStyle(Color.popcornInk)
                .contentTransition(.numericText())
            Spacer()
            monthArrow("chevron.right", label: L10n.calendarNextMonth) { viewModel.showNextMonth() }
        }
        .padding(.horizontal, 4)
    }

    // butter-light round arrow, 44 pt tap area
    private func monthArrow(_ systemImage: String, label: String, action: @escaping () -> Void) -> some View {
        Button {
            withAnimation { action() }
        } label: {
            Image(systemName: systemImage)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(Color.popcornCaramel)
                .frame(width: 36, height: 36)
                .background(Color.popcornButterLight, in: Circle())
                .frame(width: 44, height: 44)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }

    private var weekdayHeader: some View {
        HStack(spacing: 0) {
            ForEach(Array(Self.weekdays.enumerated()), id: \.offset) { index, name in
                Text(name)
                    .font(.caption.weight(.medium))
                    .foregroundStyle(index == 0 || index == 6 ? weekdayColor(column: index) : .popcornInkMuted)
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
                        // only the side pane keeps a selection, the sheet covers the grid
                        isSelected: isTwoPane && viewModel.selectedDate == date,
                        color: viewModel.holidays.contains(date) ? Self.holidayRed : weekdayColor(column: index % 7)
                    ) {
                        viewModel.selectDate(date)
                    }
                } else {
                    Color.clear.frame(height: 44)
                }
            }
        }
    }

    // sundays + public holidays
    private static let holidayRed = Color.popcornHoliday

    private func weekdayColor(column: Int) -> Color {
        switch column {
        case 0: return Self.holidayRed
        case 6: return .popcornSaturday
        default: return .popcornInk
        }
    }
}

// month swipe, today button, watched dots, shared by both layouts
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
                    Button(L10n.calendarToday) { withAnimation { viewModel.showToday() } }
                }
            }
            .task(id: viewModel.currentMonth) { await viewModel.observeWatchedDates() }
            .task(id: viewModel.currentMonth) { await viewModel.loadHolidays() }
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
                Text(verbatim: "\(day)")
                    .font(.body.weight(isToday || isSelected ? .bold : .regular))
                    .foregroundStyle(isSelected ? Color.popcornInk : color)
                    .frame(width: 34, height: 34)
                    .background {
                        if isSelected {
                            Circle().fill(Color.popcornButter)
                        } else if isToday {
                            Circle().strokeBorder(Color.popcornOutlineStrong, lineWidth: 1.5)
                        }
                    }
                Circle()
                    .fill(isWatched && !isSelected ? Color.popcornStar : Color.clear)
                    .frame(width: 5, height: 5)
            }
            .frame(maxWidth: .infinity, minHeight: 44)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityLabel(isWatched ? L10n.calendarDayWatched(day) : L10n.calendarDay(day))
        .accessibilityAddTraits(isSelected ? .isSelected : [])
    }
}

// day records, like the android date bottom sheet
private struct DateRecordsSheet: View {
    @Bindable var viewModel: CalendarViewModel

    var body: some View {
        NavigationStack {
            List {
                if viewModel.selectedDateRecords.isEmpty {
                    Text(L10n.calendarNoRecords)
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
            .popcornScreenBackground()
            .navigationTitle(viewModel.selectedDate?.displayText ?? "")
            .inlineNavigationTitle()
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(L10n.commonClose) { viewModel.dismissDate() }
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

// side pane: day records, tap to edit in place
private struct DateRecordsPane: View {
    @Bindable var viewModel: CalendarViewModel

    var body: some View {
        Group {
            if let date = viewModel.selectedDate {
                ScrollView {
                    VStack(alignment: .leading, spacing: 10) {
                        Text(date.displayText)
                            .font(.headline)
                            .foregroundStyle(Color.popcornInk)
                            .accessibilityAddTraits(.isHeader)
                        if viewModel.selectedDateRecords.isEmpty {
                            Text(L10n.calendarNoRecords)
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
                            Text(L10n.calendarEditHint)
                                .font(.footnote)
                                .foregroundStyle(Color.popcornInkMuted)
                        }
                    }
                    .padding(.vertical)
                }
            } else {
                PopcornEmptyState(title: L10n.calendarSelectDateTitle, systemImage: "calendar", message: L10n.calendarSelectDateMessage)
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
