import SwiftUI

// iOS-only modifiers behind one name, so screens compile for macOS too (package tests)
extension View {
    func inlineNavigationTitle() -> some View {
        #if os(iOS)
        navigationBarTitleDisplayMode(.inline)
        #else
        self
        #endif
    }

    // swipeable pages (photo viewer), plain tab view elsewhere
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

    // backdrop shows through the nav bar
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
