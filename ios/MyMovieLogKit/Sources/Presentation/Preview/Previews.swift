#if DEBUG
import SwiftUI

// PreviewProvider rather than #Preview: the macro plugin ships only with Xcode, and this
// package is also built with the plain Swift toolchain.
struct RootView_Previews: PreviewProvider {
    static var previews: some View {
        RootView(useCases: .preview())
            .previewDisplayName("Signed in")
        RootView(useCases: .preview(loggedIn: false))
            .previewDisplayName("Signed out")
    }
}
#endif
