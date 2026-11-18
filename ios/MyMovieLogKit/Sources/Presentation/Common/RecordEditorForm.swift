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
            Picker(L10n.recordStatus, selection: $draft.status) {
                Text(L10n.recordStatusWatched).tag(WatchStatus.watched)
                Text(L10n.recordStatusWishlist).tag(WatchStatus.wishlist)
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets())
        }

        if draft.status == .watched {
            Section(L10n.recordRating) {
                HStack {
                    StarRatingInput(rating: $draft.rating)
                    Spacer()
                    if draft.rating > 0 {
                        Button(L10n.recordRatingNone) { draft.rating = 0 }
                            .font(.footnote)
                            .buttonStyle(.borderless)
                    }
                }
            }

            Section {
                Toggle(L10n.recordWatchedDate, isOn: hasWatchedDate)
                if draft.watchedAt != nil {
                    DatePicker(
                        L10n.recordDate,
                        selection: watchedDate,
                        in: ...Date(),
                        displayedComponents: .date
                    )
                    .datePickerStyle(.graphical)
                    .environment(\.locale, L10n.locale)
                }
            }

            Section(L10n.recordReviewOptional) {
                TextField(L10n.recordReviewPlaceholder, text: $draft.review, axis: .vertical)
                    .lineLimit(3...8)
            }
        } else {
            Section(L10n.recordMemoOptional) {
                TextField(L10n.recordMemoPlaceholder, text: $draft.memo, axis: .vertical)
                    .lineLimit(3...8)
            }
        }

        if photoCount > 0 {
            Section {
                Label(L10n.recordPhotosKept(photoCount), systemImage: "photo.on.rectangle")
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
    var saveLabel = L10n.commonSave
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
                        Button(L10n.recordDelete, role: .destructive) { isConfirmingDelete = true }
                    }
                }
            }
            .navigationTitle(title)
            .inlineNavigationTitle()
            .disabled(saveState.isSaving)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.commonCancel, action: onCancel)
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
                L10n.recordDeleteConfirm(title),
                isPresented: $isConfirmingDelete,
                titleVisibility: .visible
            ) {
                Button(L10n.commonDelete, role: .destructive) { onDelete?() }
                Button(L10n.commonCancel, role: .cancel) {}
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
