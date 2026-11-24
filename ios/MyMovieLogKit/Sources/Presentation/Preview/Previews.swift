#if DEBUG
import SwiftUI

// PreviewProvider, not #Preview: the macro plugin is Xcode-only and this builds with plain swift too
struct RootView_Previews: PreviewProvider {
    static var previews: some View {
        RootView(useCases: .preview())
            .previewDisplayName("Signed in")
        RootView(useCases: .preview(loggedIn: false))
            .previewDisplayName("Signed out")
    }
}
#endif
