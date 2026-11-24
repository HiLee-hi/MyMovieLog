import Foundation
import Observation

// hardware seam, AVCameraDevice on iOS, fake in tests (runs on macOS)
@MainActor
public protocol CameraDevice: AnyObject {
    // asks when undecided, true if granted
    func requestAccess() async -> Bool
    func start() async throws
    func stop()
    // JPEG with EXIF orientation
    func capturePhoto() async throws -> Data
}

// copy of the used photo to the library, like android saving to the gallery
@MainActor
public protocol PhotoLibrarySaving {
    func save(_ jpeg: Data) async
}

// preview -> capture -> review (retake / use), same as android CameraScreen
@MainActor
@Observable
public final class CameraViewModel {
    public enum Phase: Equatable {
        case starting
        case denied
        case preview
        case capturing
        case review(Data)
        case error(String)
    }

    public private(set) var phase: Phase = .starting

    private let device: CameraDevice
    private let library: PhotoLibrarySaving?

    public init(device: CameraDevice, library: PhotoLibrarySaving?) {
        self.device = device
        self.library = library
    }

    public func start() async {
        guard await device.requestAccess() else {
            phase = .denied
            return
        }
        do {
            try await device.start()
            phase = .preview
        } catch {
            phase = .error(error.localizedDescription)
        }
    }

    public func stop() {
        device.stop()
    }

    public func capture() async {
        guard phase == .preview else { return }
        phase = .capturing
        do {
            phase = .review(try await device.capturePhoto())
        } catch {
            phase = .error(error.localizedDescription)
        }
    }

    public func retake() {
        if case .review = phase { phase = .preview }
    }

    // reviewed photo for the record, library copy in the background
    public func usePhoto() -> Data? {
        guard case .review(let data) = phase else { return nil }
        if let library {
            Task { await library.save(data) }
        }
        return data
    }
}
