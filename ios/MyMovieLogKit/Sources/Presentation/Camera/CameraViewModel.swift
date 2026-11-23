import Foundation
import Observation

/// What the camera screen needs from the hardware. The iOS implementation is `AVCameraDevice`;
/// tests use a fake, so the screen flow is tested on macOS too.
@MainActor
public protocol CameraDevice: AnyObject {
    /// Asks for camera access when undecided; true when granted
    func requestAccess() async -> Bool
    func start() async throws
    func stop()
    /// JPEG data with its EXIF orientation
    func capturePhoto() async throws -> Data
}

/// Keeps a copy of a used photo in the photo library, like the Android app saving to the gallery
@MainActor
public protocol PhotoLibrarySaving {
    func save(_ jpeg: Data) async
}

/// Preview → capture → review (retake / use), the same flow as the Android CameraScreen
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

    /// The reviewed photo for the record; a copy goes to the photo library in the background
    public func usePhoto() -> Data? {
        guard case .review(let data) = phase else { return nil }
        if let library {
            Task { await library.save(data) }
        }
        return data
    }
}
