import Domain
import SwiftUI

struct LibraryView: View {
    @Binding private var status: WatchStatus
    private let isLoggedIn: Bool
    private let onNavigateToLogin: () -> Void
    private let onRefresh: () async -> Void
    @State private var viewModel: LibraryViewModel
    @State private var pendingDelete: MovieRecord?

    init(
        useCases: UseCaseContainer,
        status: Binding<WatchStatus>,
        isLoggedIn: Bool,
        onNavigateToLogin: @escaping () -> Void,
        onRefresh: @escaping () async -> Void
    ) {
        _status = status
        self.isLoggedIn = isLoggedIn
        self.onNavigateToLogin = onNavigateToLogin
        self.onRefresh = onRefresh
        _viewModel = State(initialValue: LibraryViewModel(
            getRecords: useCases.getRecords,
            upsertRecord: useCases.upsertRecord,
            deleteRecord: useCases.deleteRecord
        ))
    }

    var body: some View {
        NavigationStack {
            Group {
                if isLoggedIn {
                    content
                } else {
                    LoginRequiredView(onNavigateToLogin: onNavigateToLogin)
                }
            }
            .navigationTitle("라이브러리")
        }
        .task { await viewModel.observeRecords() }
    }

    private var content: some View {
        VStack(spacing: 0) {
            Picker("목록", selection: $status) {
                Text("감상 완료 (\(viewModel.watchedRecords.count))").tag(WatchStatus.watched)
                Text("위시리스트 (\(viewModel.wishlistRecords.count))").tag(WatchStatus.wishlist)
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            // Stays a compact control on wide windows instead of stretching edge to edge
            .frame(maxWidth: 420)
            .padding(.horizontal)
            .padding(.bottom, 8)

            let records = viewModel.records(for: status)
            if records.isEmpty {
                ContentUnavailableView {
                    Label(status == .watched ? "아직 본 영화가 없어요" : "위시리스트가 비어 있어요", systemImage: "film")
                } description: {
                    Text("검색 탭에서 영화를 찾아 기록해보세요")
                }
            } else {
                ScrollView {
                    LazyVGrid(columns: [GridItem(.adaptive(minimum: 100), spacing: 12)], spacing: 16) {
                        ForEach(records) { record in
                            Button {
                                viewModel.editSession.select(record)
                            } label: {
                                RecordPosterCell(record: record)
                            }
                            .buttonStyle(.plain)
                            .contextMenu {
                                Button("삭제", systemImage: "trash", role: .destructive) {
                                    pendingDelete = record
                                }
                            }
                        }
                    }
                    .padding()
                    .frame(maxWidth: AdaptiveDimens.contentMaxWidth)
                    .frame(maxWidth: .infinity)
                }
                .refreshable { await onRefresh() }
            }
        }
        .sheet(isPresented: viewModel.editSession.isPresentedBinding) {
            viewModel.editSession.editorSheet(onDelete: { record in
                Task { await viewModel.deleteRecord(record) }
            })
        }
        .confirmationDialog(
            "\"\(pendingDelete?.title ?? "")\" 기록을 삭제할까요?",
            isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 { pendingDelete = nil } }),
            titleVisibility: .visible,
            presenting: pendingDelete
        ) { record in
            Button("삭제", role: .destructive) {
                Task { await viewModel.deleteRecord(record) }
            }
            Button("취소", role: .cancel) {}
        }
        .alert(
            "삭제하지 못했어요",
            isPresented: Binding(get: { viewModel.deleteError != nil }, set: { if !$0 { viewModel.clearDeleteError() } })
        ) {
            Button("확인", role: .cancel) {}
        } message: {
            Text(viewModel.deleteError ?? "")
        }
    }
}
