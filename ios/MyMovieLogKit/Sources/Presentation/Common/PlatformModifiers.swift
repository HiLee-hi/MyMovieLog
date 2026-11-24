import SwiftUI

// iOS-only modifiers behind one name each, so the screens also compile for macOS
// (where the package is built and tested without a simulator).
extension View {
    func inlineNavigationTitle() -> some View {
        #if os(iOS)
        navigationBarTitleDisplayMode(.inline)
        #else
        self
        #endif
    }

    /// Swipeable pages (the photo viewer); a plain tab view elsewhere
    func pagedTabViewStyle() -> some View {
        #if os(iOS)
        tabViewStyle(.page(indexDisplayMode: .automatic))
        #else
        self
        #endif
    }

    func hidesNavigationBar(_ hidden: Bool) -> some View {
        #if os(iOS)
        toolbar(hidden ? .hidden : .visible, for: .navigationBar)
        #else
        self
        #endif
    }

    /// Lets the content (a backdrop image) show through the navigation bar
    func transparentNavigationBar() -> some View {
        #if os(iOS)
        toolbarBackground(.hidden, for: .navigationBar)
        #else
        self
        #endif
    }

    func emailInput() -> some View {
        #if os(iOS)
        keyboardType(.emailAddress)
            .textContentType(.emailAddress)
            .textInputAutocapitalization(.never)
            .autocorrectionDisabled()
        #else
        autocorrectionDisabled()
        #endif
    }

    func passwordInput(isNew: Bool) -> some View {
        #if os(iOS)
        textContentType(isNew ? .newPassword : .password)
        #else
        textContentType(.password)
        #endif
    }
}
