#if os(iOS)
import AVFoundation
import Photos
import UIKit

enum CameraError: LocalizedError {
    case unavailable
    case noImageData

    var errorDescription: String? {
        switch self {
        case .unavailable: return L10n.cameraUnavailable
        case .noImageData: return L10n.cameraCaptureFailed
        }
    }
}

// back camera via AVFoundation, session on its own queue to keep the UI free
// rotation coordinator keeps preview / photo upright
@MainActor
final class AVCameraDevice: NSObject, CameraDevice {
    static var isAvailable: Bool {
        AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) != nil
    }

    let session = AVCaptureSession()
    private let output = AVCapturePhotoOutput()
    private let sessionQueue = DispatchQueue(label: "com.mymovie.log.camera")
    private var isConfigured = false
    private var device: AVCaptureDevice?
    private var rotationCoordinator: AVCaptureDevice.RotationCoordinator?
    private var pendingCapture: CheckedContinuation<Data, Error>?

    func requestAccess() async -> Bool {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized: return true
        case .notDetermined: return await AVCaptureDevice.requestAccess(for: .video)
        default: return false
        }
    }

    func start() async throws {
        if !isConfigured {
            guard let camera = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .back) else {
                throw CameraError.unavailable
            }
            let input = try AVCaptureDeviceInput(device: camera)
            session.beginConfiguration()
            session.sessionPreset = .photo
            guard session.canAddInput(input), session.canAddOutput(output) else {
                session.commitConfiguration()
                throw CameraError.unavailable
            }
            session.addInput(input)
            session.addOutput(output)
            output.maxPhotoQualityPrioritization = .balanced
            session.commitConfiguration()
            device = camera
            isConfigured = true
        }
        let session = session
        await withCheckedContinuation { (continuation: CheckedContinuation<Void, Never>) in
            sessionQueue.async {
                if !session.isRunning { session.startRunning() }
                continuation.resume()
            }
        }
    }

    func stop() {
        let session = session
        sessionQueue.async {
            if session.isRunning { session.stopRunning() }
        }
    }

    // called on every preview layout, incl. rotation
    func updatePreviewRotation(_ layer: AVCaptureVideoPreviewLayer) {
        guard let device else { return }
        if rotationCoordinator?.previewLayer !== layer {
            rotationCoordinator = AVCaptureDevice.RotationCoordinator(device: device, previewLayer: layer)
        }
        guard let angle = rotationCoordinator?.videoRotationAngleForHorizonLevelPreview,
              let connection = layer.connection, connection.isVideoRotationAngleSupported(angle)
        else { return }
        connection.videoRotationAngle = angle
    }

    func capturePhoto() async throws -> Data {
        if let connection = output.connection(with: .video), let rotationCoordinator {
            let angle = rotationCoordinator.videoRotationAngleForHorizonLevelCapture
            if connection.isVideoRotationAngleSupported(angle) { connection.videoRotationAngle = angle }
        }
        return try await withCheckedThrowingContinuation { continuation in
            pendingCapture = continuation
            output.capturePhoto(with: AVCapturePhotoSettings(format: [AVVideoCodecKey: AVVideoCodecType.jpeg]), delegate: self)
        }
    }

    private func finishCapture(_ result: Result<Data, Error>) {
        pendingCapture?.resume(with: result)
        pendingCapture = nil
    }
}

extension AVCameraDevice: AVCapturePhotoCaptureDelegate {
    nonisolated func photoOutput(_ output: AVCapturePhotoOutput, didFinishProcessingPhoto photo: AVCapturePhoto, error: Error?) {
        let result: Result<Data, Error>
        if let error {
            result = .failure(error)
        } else if let data = photo.fileDataRepresentation() {
            result = .success(data)
        } else {
            result = .failure(CameraError.noImageData)
        }
        Task { @MainActor in self.finishCapture(result) }
    }
}

// add-only access, no library reads, refusal just skips the copy
struct PhotoLibrarySaver: PhotoLibrarySaving {
    func save(_ jpeg: Data) async {
        await Self.addToLibrary(jpeg)
    }

    // nonisolated on purpose: Photos runs the block on its own queue
    // a main-actor block would trap on the isolation check
    private nonisolated static func addToLibrary(_ jpeg: Data) async {
        let status = await PHPhotoLibrary.requestAuthorization(for: .addOnly)
        guard status == .authorized || status == .limited else { return }
        try? await PHPhotoLibrary.shared().performChanges {
            PHAssetCreationRequest.forAsset().addResource(with: .photo, data: jpeg, options: nil)
        }
    }
}
#endif
