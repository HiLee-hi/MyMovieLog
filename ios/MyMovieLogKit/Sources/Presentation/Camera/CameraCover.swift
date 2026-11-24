import SwiftUI
#if os(iOS)
import AVFoundation
#endif

extension View {
    // full-screen camera returning the used photo, no-op without a camera
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
    // false on simulator / Macs without a back camera -> button hidden
    @MainActor static var isAvailable: Bool {
        #if os(iOS)
        AVCameraDevice.isAvailable
        #else
        false
        #endif
    }
}
