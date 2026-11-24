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
            .navigationTitle(L10n.tabLibrary)
        }
        .task { await viewModel.observeRecords() }
    }

    private var content: some View {
        VStack(spacing: 0) {
            PopcornToggle(options: [WatchStatus.watched, .wishlist], selection: $status) { status in
                status == .watched
                    ? L10n.libraryWatchedTab(viewModel.watchedRecords.count)
                    : L10n.libraryWishlistTab(viewModel.wishlistRecords.count)
            }
            .accessibilityElement(children: .contain)
            .accessibilityLabel(L10n.libraryList)
            // compact control on wide windows, no edge-to-edge stretch
            .frame(maxWidth: 420)
            .padding(.horizontal)
            .padding(.bottom, 8)

            let records = viewModel.records(for: status)
            if records.isEmpty {
                PopcornEmptyState(
                    title: status == .watched ? L10n.libraryWatchedEmpty : L10n.libraryWishlistEmpty,
                    systemImage: "film",
                    message: L10n.libraryEmptyHint
                )
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
                                Button(L10n.commonDelete, systemImage: "trash", role: .destructive) {
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
            L10n.recordDeleteConfirm(pendingDelete?.title ?? ""),
            isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 { pendingDelete = nil } }),
            titleVisibility: .visible,
            presenting: pendingDelete
        ) { record in
            Button(L10n.commonDelete, role: .destructive) {
                Task { await viewModel.deleteRecord(record) }
            }
            Button(L10n.commonCancel, role: .cancel) {}
        }
        .alert(
            L10n.libraryDeleteFailed,
            isPresented: Binding(get: { viewModel.deleteError != nil }, set: { if !$0 { viewModel.clearDeleteError() } })
        ) {
            Button(L10n.commonConfirm, role: .cancel) {}
        } message: {
            Text(viewModel.deleteError ?? "")
        }
    }
}
