import Domain
import SwiftUI

/// The record form (status, rating, watched date, review or memo) as Form sections.
/// Shown in a sheet from every screen that adds or edits a record.
struct RecordEditorForm: View {
    @Binding var draft: RecordDraft
    let saveState: SaveState
    var photoCount = 0

    var body: some View {
        Section {
            Picker("상태", selection: $draft.status) {
                Text("봤어요").tag(WatchStatus.watched)
                Text("보고싶어요").tag(WatchStatus.wishlist)
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets())
        }

        if draft.status == .watched {
            Section("별점") {
                HStack {
                    StarRatingInput(rating: $draft.rating)
                    Spacer()
                    if draft.rating > 0 {
                        Button("없음") { draft.rating = 0 }
                            .font(.footnote)
                            .buttonStyle(.borderless)
                    }
                }
            }

            Section {
                Toggle("감상한 날짜", isOn: hasWatchedDate)
                if draft.watchedAt != nil {
                    DatePicker(
                        "날짜",
                        selection: watchedDate,
                        in: ...Date(),
                        displayedComponents: .date
                    )
                    .datePickerStyle(.graphical)
                    .environment(\.locale, Locale(identifier: "ko_KR"))
                }
            }

            Section("리뷰 (선택)") {
                TextField("어땠나요?", text: $draft.review, axis: .vertical)
                    .lineLimit(3...8)
            }
        } else {
            Section("메모 (선택)") {
                TextField("보고 싶은 이유를 남겨보세요", text: $draft.memo, axis: .vertical)
                    .lineLimit(3...8)
            }
        }

        if photoCount > 0 {
            Section {
                Label("첨부된 사진 \(photoCount)장은 그대로 유지돼요", systemImage: "photo.on.rectangle")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
            }
        }

        if let message = saveState.errorMessage {
            Section {
                Text(message)
                    .font(.footnote)
                    .foregroundStyle(.red)
            }
        }
    }

    private var hasWatchedDate: Binding<Bool> {
        Binding(
            get: { draft.watchedAt != nil },
            set: { draft.watchedAt = $0 ? (draft.watchedAt ?? .today()) : nil }
        )
    }

    private var watchedDate: Binding<Date> {
        Binding(
            get: { (draft.watchedAt ?? .today()).date() },
            set: { draft.watchedAt = LocalDate($0) }
        )
    }
}

/// Sheet around `RecordEditorForm` with cancel / save buttons
struct RecordEditorSheet: View {
    let title: String
    @Binding var draft: RecordDraft
    let saveState: SaveState
    var saveLabel = "저장"
    var photoCount = 0
    var onDelete: (() -> Void)?
    let onSave: () async -> Void
    let onCancel: () -> Void

    @State private var isConfirmingDelete = false

    var body: some View {
        NavigationStack {
            Form {
                RecordEditorForm(draft: $draft, saveState: saveState, photoCount: photoCount)

                if onDelete != nil {
                    Section {
                        Button("기록 삭제", role: .destructive) { isConfirmingDelete = true }
                    }
                }
            }
            .navigationTitle(title)
            .inlineNavigationTitle()
            .disabled(saveState.isSaving)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("취소", action: onCancel)
                }
                ToolbarItem(placement: .confirmationAction) {
                    if saveState.isSaving {
                        ProgressView()
                    } else {
                        Button(saveLabel) { Task { await onSave() } }
                            .fontWeight(.semibold)
                    }
                }
            }
            .confirmationDialog(
                "\"\(title)\" 기록을 삭제할까요?",
                isPresented: $isConfirmingDelete,
                titleVisibility: .visible
            ) {
                Button("삭제", role: .destructive) { onDelete?() }
                Button("취소", role: .cancel) {}
            }
        }
        .interactiveDismissDisabled(saveState.isSaving)
    }
}

extension RecordEditSession {
    /// Sheet for the selected record; it closes itself once the save succeeds
    func editorSheet(onDelete: ((MovieRecord) -> Void)? = nil) -> some View {
        RecordEditSessionSheet(session: self, onDelete: onDelete)
    }

    /// For `.sheet(isPresented:)`
    var isPresentedBinding: Binding<Bool> {
        Binding(
            get: { self.record != nil },
            set: { if !$0 { self.clear() } }
        )
    }
}

private struct RecordEditSessionSheet: View {
    @Bindable var session: RecordEditSession
    let onDelete: ((MovieRecord) -> Void)?

    var body: some View {
        if let record = session.record {
            RecordEditorSheet(
                title: record.title,
                draft: $session.draft,
                saveState: session.saveState,
                photoCount: record.photoUrls.count,
                onDelete: onDelete.map { onDelete in { onDelete(record) } },
                onSave: {
                    await session.save()
                    if session.saveState == .success { session.clear() }
                },
                onCancel: { session.clear() }
            )
        }
    }
}
