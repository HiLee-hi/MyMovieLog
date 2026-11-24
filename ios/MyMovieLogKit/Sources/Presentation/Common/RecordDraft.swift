import Domain
import Foundation

/// A photo picked in this editing session, not uploaded yet
public struct AttachedPhoto: Identifiable, Equatable, Sendable {
    public let id: UUID
    /// Image data as the picker or camera delivered it; resized when uploaded
    public let data: Data
    /// Photo library item identifier for album picks; nil for camera photos
    public let sourceId: String?

    public init(id: UUID = UUID(), data: Data, sourceId: String? = nil) {
        self.id = id
        self.data = data
        self.sourceId = sourceId
    }
}

/// In-progress record form values. Held by ViewModels, not by the form view, so a draft
/// survives the sheet being re-created (rotation, iPad multitasking resizes).
public struct RecordDraft: Equatable, Sendable {
    public var status: WatchStatus
    public var rating: Double
    public var watchedAt: LocalDate?
    public var review: String
    public var memo: String
    /// Storage paths of the record's photos that stay attached
    public var keptPhotoPaths: [String]
    /// Photos added in this session, uploaded on save
    public var newPhotos: [AttachedPhoto]

    /// Same limit as the Android editor
    public static let maxPhotos = 10

    public init(
        status: WatchStatus = .watched,
        rating: Double = 0,
        watchedAt: LocalDate? = nil,
        review: String = "",
        memo: String = "",
        keptPhotoPaths: [String] = [],
        newPhotos: [AttachedPhoto] = []
    ) {
        self.status = status
        self.rating = rating
        self.watchedAt = watchedAt
        self.review = review
        self.memo = memo
        self.keptPhotoPaths = keptPhotoPaths
        self.newPhotos = newPhotos
    }

    public var photoCount: Int { keptPhotoPaths.count + newPhotos.count }
    public var remainingPhotoSlots: Int { max(Self.maxPhotos - photoCount, 0) }

    /// Adds as many as fit under the limit
    public mutating func attach(_ images: [Data]) {
        attach(images.map { AttachedPhoto(data: $0) })
    }

    /// Adds as many as fit under the limit, skipping photos that are already attached: the same
    /// library item (`knownSourceIds` covers the saved photos), or the same data when the item is
    /// unknown. Returns how many were skipped as duplicates.
    @discardableResult
    public mutating func attach(_ photos: [AttachedPhoto], knownSourceIds: Set<String> = []) -> Int {
        var seenSources = knownSourceIds.union(newPhotos.compactMap(\.sourceId))
        var fresh: [AttachedPhoto] = []
        var skipped = 0
        for photo in photos {
            let isDuplicate = if let sourceId = photo.sourceId {
                seenSources.contains(sourceId)
            } else {
                (newPhotos + fresh).contains { $0.data == photo.data }
            }
            if isDuplicate {
                skipped += 1
                continue
            }
            if let sourceId = photo.sourceId { seenSources.insert(sourceId) }
            fresh.append(photo)
        }
        newPhotos += fresh.prefix(remainingPhotoSlots)
        return skipped
    }

    public init(record: MovieRecord?) {
        self.init(
            status: record?.status ?? .watched,
            rating: record?.rating ?? 0,
            watchedAt: record?.watchedAt,
            review: record?.review ?? "",
            memo: record?.memo ?? "",
            keptPhotoPaths: record?.photoUrls ?? []
        )
    }

    /// Writes the form into a record the same way the Android editor does: a watched record
    /// keeps rating, date and review; a wishlist record keeps only the memo. Blank text and a
    /// zero rating are stored as null.
    public func applied(to record: MovieRecord) -> MovieRecord {
        var updated = record
        let watched = status == .watched
        updated.status = status
        updated.rating = watched && rating > 0 ? rating : nil
        updated.watchedAt = watched ? watchedAt : nil
        updated.review = watched ? review.nonBlank : nil
        updated.memo = watched ? nil : memo.nonBlank
        // New photos are appended by UpsertRecordUseCase once they are uploaded
        updated.photoUrls = keptPhotoPaths
        return updated
    }
}

public enum SaveState: Equatable, Sendable {
    case idle
    case saving
    case success
    case error(String)

    public var isSaving: Bool { self == .saving }

    public var errorMessage: String? {
        if case .error(let message) = self { return message }
        return nil
    }
}

extension String {
    var nonBlank: String? {
        trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ? nil : self
    }
}
