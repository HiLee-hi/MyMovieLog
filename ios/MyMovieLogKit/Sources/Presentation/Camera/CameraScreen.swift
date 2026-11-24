#if os(iOS)
import AVFoundation
import SwiftUI
import UIKit

/// Full-screen camera from the design canvas: close button, the "티켓이나 포스터를 찍어 보세요"
/// hint and a butter shutter; after a shot, retake or use it.
struct CameraScreen: View {
    let onUse: (Data) -> Void
    let onClose: () -> Void

    @State private var device = AVCameraDevice()
    @State private var viewModel: CameraViewModel?
    @Environment(\.openURL) private var openURL

    var body: some View {
        ZStack {
            Color(hex: 0x2E2218).ignoresSafeArea()
            if let viewModel {
                content(viewModel)
            }
        }
        .task {
            let model = CameraViewModel(device: device, library: PhotoLibrarySaver())
            viewModel = model
            await model.start()
        }
        .onDisappear { viewModel?.stop() }
    }

    @ViewBuilder
    private func content(_ viewModel: CameraViewModel) -> some View {
        switch viewModel.phase {
        case .starting:
            ProgressView().tint(.popcornButterLight)
        case .preview, .capturing:
            CameraPreview(device: device)
                .ignoresSafeArea()
                .overlay(alignment: .top) { topBar(showsHint: true) }
                .overlay(alignment: .bottom) { shutter(viewModel) }
        case .review(let data):
            review(data, viewModel: viewModel)
        case .denied:
            message(L10n.cameraDenied, action: (L10n.cameraOpenSettings, {
                if let url = URL(string: UIApplication.openSettingsURLString) { openURL(url) }
            }))
        case .error(let text):
            message(text, action: nil)
        }
    }

    private func topBar(showsHint: Bool) -> some View {
        ZStack {
            if showsHint {
                Text(L10n.cameraHint)
                    .font(.caption)
                    .foregroundStyle(Color.popcornButterLight)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 5)
                    .background(Color.black.opacity(0.35), in: Capsule())
                    .padding(.horizontal, 64)
            }
            HStack {
                Button(action: onClose) {
                    Image(systemName: "xmark")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(Color.popcornInk)
                        .frame(width: 44, height: 44)
                        .background(Color.popcornCream.opacity(0.92), in: Circle())
                }
                .accessibilityLabel(L10n.commonClose)
                Spacer()
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 8)
    }

    private func shutter(_ viewModel: CameraViewModel) -> some View {
        Button {
            Task { await viewModel.capture() }
        } label: {
            Circle()
                .fill(Color.popcornButter)
                .padding(5)
                .overlay { Circle().strokeBorder(Color.popcornCream, lineWidth: 4) }
                .frame(width: 78, height: 78)
                .overlay {
                    if viewModel.phase == .capturing { ProgressView().tint(.popcornInk) }
                }
        }
        .buttonStyle(.plain)
        .disabled(viewModel.phase != .preview)
        .accessibilityLabel(L10n.cameraShutter)
        .padding(.bottom, 32)
    }

    private func review(_ data: Data, viewModel: CameraViewModel) -> some View {
        VStack(spacing: 20) {
            Group {
                if let image = UIImage(data: data) {
                    Image(uiImage: image).resizable().scaledToFit()
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: PopcornRadius.large))
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .padding(.horizontal, 24)
            .padding(.top, 24)
            .accessibilityLabel(L10n.cameraCapturedPhoto)

            HStack(spacing: 12) {
                Button(L10n.cameraRetake) { viewModel.retake() }
                    .font(.body.weight(.bold))
                    .foregroundStyle(Color.popcornCream)
                    .frame(maxWidth: .infinity, minHeight: 52)
                    .overlay { Capsule().strokeBorder(Color.popcornCream.opacity(0.5)) }
                    .contentShape(Capsule())
                Button(L10n.cameraUsePhoto) {
                    if let photo = viewModel.usePhoto() { onUse(photo) }
                }
                .buttonStyle(.popcornPrimary)
            }
            .padding(.horizontal, 20)
            .padding(.bottom, 24)
        }
    }

    private func message(_ text: String, action: (String, () -> Void)?) -> some View {
        VStack(spacing: 16) {
            Text(text)
                .multilineTextAlignment(.center)
                .foregroundStyle(Color.popcornCream)
            if let action {
                Button(action.0, action: action.1)
                    .buttonStyle(.popcornPrimary)
                    .frame(maxWidth: 240)
            }
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .overlay(alignment: .top) { topBar(showsHint: false) }
    }
}

/// Live preview layer of the capture session
private struct CameraPreview: UIViewRepresentable {
    let device: AVCameraDevice

    func makeUIView(context: Context) -> PreviewView {
        let view = PreviewView()
        view.previewLayer.session = device.session
        view.previewLayer.videoGravity = .resizeAspectFill
        view.onLayout = { [device] layer in device.updatePreviewRotation(layer) }
        return view
    }

    func updateUIView(_ uiView: PreviewView, context: Context) {}

    final class PreviewView: UIView {
        override class var layerClass: AnyClass { AVCaptureVideoPreviewLayer.self }
        var previewLayer: AVCaptureVideoPreviewLayer { layer as! AVCaptureVideoPreviewLayer }
        var onLayout: ((AVCaptureVideoPreviewLayer) -> Void)?

        override func layoutSubviews() {
            super.layoutSubviews()
            onLayout?(previewLayer)
        }
    }
}
#endif
