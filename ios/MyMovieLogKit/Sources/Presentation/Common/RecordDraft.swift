import Domain
import Foundation

// picked this session, not uploaded yet
public struct AttachedPhoto: Identifiable, Equatable, Sendable {
    public let id: UUID
    // raw picker / camera data, resized on upload
    public let data: Data
    // library item id for album picks, nil for camera
    public let sourceId: String?

    public init(id: UUID = UUID(), data: Data, sourceId: String? = nil) {
        self.id = id
        self.data = data
        self.sourceId = sourceId
    }
}

// draft held by ViewModels, not the view, survives sheet re-creation (rotate, iPad resize)
public struct RecordDraft: Equatable, Sendable {
    public var status: WatchStatus
    public var rating: Double
    public var watchedAt: LocalDate?
    public var review: String
    public var memo: String
    // saved photo paths that stay
    public var keptPhotoPaths: [String]
    // added this session, uploaded on save
    public var newPhotos: [AttachedPhoto]

    // same limit as android
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

    // up to the limit
    public mutating func attach(_ images: [Data]) {
        attach(images.map { AttachedPhoto(data: $0) })
    }

    // up to the limit, skips duplicates: same library item (knownSourceIds = saved) or same data
    // returns skipped count
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

    // same as android: watched keeps rating / date / review, wishlist keeps memo only
    // blank text and 0 rating -> null
    public func applied(to record: MovieRecord) -> MovieRecord {
        var updated = record
        let watched = status == .watched
        updated.status = status
        updated.rating = watched && rating > 0 ? rating : nil
        updated.watchedAt = watched ? watchedAt : nil
        updated.review = watched ? review.nonBlank : nil
        updated.memo = watched ? nil : memo.nonBlank
        // UpsertRecordUseCase appends new photos after upload
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
