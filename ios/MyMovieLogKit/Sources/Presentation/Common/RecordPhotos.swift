import Domain
import Foundation
import ImageIO
import Observation
import PhotosUI
import SwiftUI

// signed URLs expire after an hour -> fetched on each editor open, not stored
@MainActor
@Observable
public final class RecordPhotosViewModel {
    public enum State: Equatable {
        case idle
        case loading
        // missing path = couldn't be signed
        case loaded([String: URL])
        case failed(String)
    }

    public private(set) var state: State = .idle

    private let getSignedPhotoUrls: GetSignedPhotoUrlsUseCase

    public init(getSignedPhotoUrls: GetSignedPhotoUrlsUseCase) {
        self.getSignedPhotoUrls = getSignedPhotoUrls
    }

    public func load(paths: [String]) async {
        guard !paths.isEmpty else {
            state = .loaded([:])
            return
        }
        state = .loading
        do {
            let urls = try await getSignedPhotoUrls(paths: paths)
            guard !Task.isCancelled else { return }
            state = .loaded(urls)
        } catch {
            guard !Task.isCancelled else { return }
            state = .failed(error.localizedDescription)
        }
    }
}

// signer for every editor without passing it down, set by RootView
// missing -> saved photos show placeholders
private struct SignedPhotoUrlsKey: EnvironmentKey {
    static let defaultValue: GetSignedPhotoUrlsUseCase? = nil
}

extension EnvironmentValues {
    var getSignedPhotoUrls: GetSignedPhotoUrlsUseCase? {
        get { self[SignedPhotoUrlsKey.self] }
        set { self[SignedPhotoUrlsKey.self] = newValue }
    }
}

// "사진" section, like android PhotoAttachSection: album add, remove, max 10
// nothing hits Storage until save
struct RecordPhotosSection: View {
    @Binding var draft: RecordDraft
    // saved paths, signed once on open
    let savedPaths: [String]
    @Environment(\.getSignedPhotoUrls) private var getSignedPhotoUrls

    var body: some View {
        Section {
            RecordPhotoEditor(draft: $draft, savedPaths: savedPaths, getSignedPhotoUrls: getSignedPhotoUrls)
                .listRowInsets(EdgeInsets(top: 12, leading: 16, bottom: 12, trailing: 16))
        } header: {
            HStack {
                Text(L10n.recordPhotos)
                Spacer()
                Text(verbatim: L10n.recordPhotoCount(draft.photoCount, RecordDraft.maxPhotos))
                    .monospacedDigit()
            }
        }
    }
}

// saved (path) or picked this session
private enum PhotoItem: Identifiable, Equatable {
    case saved(path: String)
    case new(AttachedPhoto)

    var id: String {
        switch self {
        case .saved(let path): return path
        case .new(let photo): return photo.id.uuidString
        }
    }
}

private struct RecordPhotoEditor: View {
    @Binding var draft: RecordDraft
    let savedPaths: [String]
    @State private var viewModel: RecordPhotosViewModel?
    @State private var pickerItems: [PhotosPickerItem] = []
    @State private var isImporting = false
    // skipped on the last pick, already attached
    @State private var skippedDuplicates = 0
    @State private var viewerStart: PhotoViewerStart?
    @State private var showsCamera = false

    init(draft: Binding<RecordDraft>, savedPaths: [String], getSignedPhotoUrls: GetSignedPhotoUrlsUseCase?) {
        _draft = draft
        self.savedPaths = savedPaths
        _viewModel = State(initialValue: getSignedPhotoUrls.map { RecordPhotosViewModel(getSignedPhotoUrls: $0) })
    }

    private var items: [PhotoItem] {
        draft.keptPhotoPaths.map { .saved(path: $0) } + draft.newPhotos.map { .new($0) }
    }

    private var signedURLs: [String: URL] {
        if case .loaded(let urls) = viewModel?.state { return urls }
        return [:]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 10) {
                    if draft.remainingPhotoSlots > 0 {
                        if CameraSupport.isAvailable {
                            cameraButton
                        }
                        albumButton
                    }
                    ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                        thumbnail(item, index: index)
                    }
                }
                // room for the corner remove buttons
                .padding(.top, 6)
                .padding(.trailing, 6)
            }
            if skippedDuplicates > 0 {
                Label(L10n.recordPhotoDuplicatesSkipped(skippedDuplicates), systemImage: "checkmark.circle")
                    .font(.footnote)
                    .foregroundStyle(Color.popcornInkMuted)
            }
            if case .failed = viewModel?.state {
                HStack {
                    Label(L10n.recordPhotosLoadFailed, systemImage: "exclamationmark.triangle")
                        .font(.footnote)
                        .foregroundStyle(Color.popcornInkMuted)
                    Spacer()
                    Button(L10n.commonRetry) {
                        Task { await viewModel?.load(paths: savedPaths) }
                    }
                    .buttonStyle(.borderless)
                    .font(.footnote.weight(.semibold))
                }
            }
        }
        .task(id: savedPaths) { await viewModel?.load(paths: savedPaths) }
        .onChange(of: pickerItems) { _, picked in
            guard !picked.isEmpty else { return }
            // take the batch once so a repeated change can't import twice
            pickerItems = []
            Task { await importPicked(picked) }
        }
        .cameraCover(isPresented: $showsCamera) { photo in
            skippedDuplicates = 0
            draft.attach([photo])
        }
        .sheet(item: $viewerStart) { start in
            PhotoViewer(items: items, signedURLs: signedURLs, startIndex: start.index)
        }
    }

    private var cameraButton: some View {
        Button {
            showsCamera = true
        } label: {
            AddPhotoTile(title: L10n.recordPhotoCamera, systemImage: "camera", size: Self.thumbnailSize)
        }
        .buttonStyle(.plain)
    }

    private var albumButton: some View {
        PhotosPicker(
            selection: $pickerItems,
            maxSelectionCount: draft.remainingPhotoSlots,
            selectionBehavior: .ordered,
            matching: .images,
            // library id per pick (no library access needed) for duplicate checks
            photoLibrary: .shared()
        ) {
            AddPhotoTile(title: L10n.recordPhotoAlbum, systemImage: "photo.on.rectangle", size: Self.thumbnailSize, isBusy: isImporting)
        }
        .buttonStyle(.plain)
        .disabled(isImporting)
    }

    private func thumbnail(_ item: PhotoItem, index: Int) -> some View {
        Button {
            viewerStart = PhotoViewerStart(index: index)
        } label: {
            PhotoImage(item: item, signedURL: signedURL(for: item), isLoadingURL: viewModel?.state == .loading,
                       contentMode: .fill, thumbnailSize: Self.thumbnailSize * 2)
                .frame(width: Self.thumbnailSize, height: Self.thumbnailSize)
                .background(Color.popcornButterLight)
                .clipShape(RoundedRectangle(cornerRadius: PopcornRadius.medium))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(L10n.recordPhotoNumber(index + 1))
        .overlay(alignment: .topTrailing) {
            Button {
                withAnimation { remove(item) }
            } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 22, height: 22)
                    .background(Color.popcornInk, in: Circle())
                    // tap area bigger than the dot
                    .frame(width: 44, height: 44)
                    .contentShape(Rectangle())
            }
            .buttonStyle(.plain)
            .offset(x: 17, y: -17)
            .accessibilityLabel(L10n.recordPhotoRemove(index + 1))
        }
    }

    private func signedURL(for item: PhotoItem) -> URL? {
        if case .saved(let path) = item { return signedURLs[path] }
        return nil
    }

    private func remove(_ item: PhotoItem) {
        switch item {
        case .saved(let path): draft.keptPhotoPaths.removeAll { $0 == path }
        case .new(let photo): draft.newPhotos.removeAll { $0.id == photo.id }
        }
    }

    private func importPicked(_ picked: [PhotosPickerItem]) async {
        isImporting = true
        defer { isImporting = false }
        let known = PhotoSourceIndex.shared.sourceIds(for: draft.keptPhotoPaths)
            .union(draft.newPhotos.compactMap(\.sourceId))
        var photos: [AttachedPhoto] = []
        var skipped = 0
        for item in picked {
            // skip known items before loading data
            if let sourceId = item.itemIdentifier, known.contains(sourceId) {
                skipped += 1
                continue
            }
            if let data = try? await item.loadTransferable(type: Data.self) {
                photos.append(AttachedPhoto(data: data, sourceId: item.itemIdentifier))
            }
        }
        skipped += draft.attach(photos, knownSourceIds: known)
        skippedDuplicates = skipped
    }

    static let thumbnailSize: CGFloat = 76
}

// dashed butter tile for camera / album buttons
private struct AddPhotoTile: View {
    let title: String
    let systemImage: String
    let size: CGFloat
    var isBusy = false

    var body: some View {
        VStack(spacing: 4) {
            if isBusy {
                ProgressView()
            } else {
                Image(systemName: systemImage)
                    .font(.system(size: 22))
            }
            Text(title)
                .font(.caption.weight(.medium))
        }
        .foregroundStyle(Color.popcornCaramel)
        .frame(width: size, height: size)
        .background(Color.popcornButterLight, in: RoundedRectangle(cornerRadius: PopcornRadius.medium))
        .overlay {
            RoundedRectangle(cornerRadius: PopcornRadius.medium)
                .strokeBorder(Color.popcornOutlineStrong, style: StrokeStyle(lineWidth: 1.5, dash: [4, 3]))
        }
    }
}

private struct PhotoViewerStart: Identifiable {
    let index: Int
    var id: Int { index }
}

// saved via signed URL, picked via its data
private struct PhotoImage: View {
    let item: PhotoItem
    let signedURL: URL?
    let isLoadingURL: Bool
    let contentMode: ContentMode
    // decode size for picked photos, nil = full size
    let thumbnailSize: CGFloat?

    var body: some View {
        switch item {
        case .saved:
            if let signedURL {
                AsyncImage(url: signedURL) { phase in
                    switch phase {
                    case .success(let image): image.resizable().aspectRatio(contentMode: contentMode)
                    case .failure: placeholderIcon
                    default: ProgressView()
                    }
                }
            } else if isLoadingURL {
                ProgressView()
            } else {
                placeholderIcon
            }
        case .new(let photo):
            LocalPhoto(photo: photo, contentMode: contentMode, maxPixelSize: thumbnailSize)
        }
    }

    private var placeholderIcon: some View {
        Image(systemName: "photo")
            .foregroundStyle(Color.popcornOutlineStrong)
    }
}

// decode off main thread, downscaled for thumbnails
private struct LocalPhoto: View {
    let photo: AttachedPhoto
    let contentMode: ContentMode
    let maxPixelSize: CGFloat?
    @State private var image: CGImage?

    var body: some View {
        Group {
            if let image {
                Image(decorative: image, scale: 1).resizable().aspectRatio(contentMode: contentMode)
            } else {
                ProgressView()
            }
        }
        .task(id: photo.id) {
            let data = photo.data
            let size = maxPixelSize
            image = await Task.detached(priority: .userInitiated) { Self.decode(data, maxPixelSize: size) }.value
        }
    }

    nonisolated private static func decode(_ data: Data, maxPixelSize: CGFloat?) -> CGImage? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil) else { return nil }
        var options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
        ]
        if let maxPixelSize { options[kCGImageSourceThumbnailMaxPixelSize] = Int(maxPixelSize) }
        return CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary)
    }
}

// full-size swipe viewer
private struct PhotoViewer: View {
    let items: [PhotoItem]
    let signedURLs: [String: URL]
    @State private var selection: Int
    @Environment(\.dismiss) private var dismiss

    init(items: [PhotoItem], signedURLs: [String: URL], startIndex: Int) {
        self.items = items
        self.signedURLs = signedURLs
        _selection = State(initialValue: startIndex)
    }

    var body: some View {
        NavigationStack {
            TabView(selection: $selection) {
                ForEach(Array(items.enumerated()), id: \.element.id) { index, item in
                    PhotoImage(item: item, signedURL: url(for: item), isLoadingURL: false,
                               contentMode: .fit, thumbnailSize: nil)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                        .tag(index)
                        .accessibilityLabel(L10n.recordPhotoNumber(index + 1))
                }
            }
            .pagedTabViewStyle()
            .background(Color.black)
            .navigationTitle(L10n.recordPhotoPosition(selection + 1, items.count))
            .inlineNavigationTitle()
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(L10n.commonClose) { dismiss() }
                }
            }
        }
    }

    private func url(for item: PhotoItem) -> URL? {
        if case .saved(let path) = item { return signedURLs[path] }
        return nil
    }
}
