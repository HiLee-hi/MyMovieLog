import SwiftUI
#if os(iOS)
import AVFoundation
#endif

extension View {
    /// Full-screen camera that hands back the used photo; nothing on platforms without one
    func cameraCover(isPresented: Binding<Bool>, onUse: @escaping (Data) -> Void) -> some View {
        #if os(iOS)
        fullScreenCover(isPresented: isPresented) {
            CameraScreen(
                onUse: { data in
                    onUse(data)
                    isPresented.wrappedValue = false
                },
                onClose: { isPresented.wrappedValue = false }
            )
        }
        #else
        self
        #endif
    }
}

enum CameraSupport {
    /// False on the simulator and Macs without a back camera, where the camera button is hidden
    @MainActor static var isAvailable: Bool {
        #if os(iOS)
        AVCameraDevice.isAvailable
        #else
        false
        #endif
    }
}
