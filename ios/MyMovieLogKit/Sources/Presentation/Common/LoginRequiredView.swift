import SwiftUI

struct LoginRequiredView: View {
    var message = L10n.loginRequiredTitle
    let onNavigateToLogin: () -> Void

    var body: some View {
        PopcornEmptyState(
            title: message,
            systemImage: "lock",
            message: L10n.loginRequiredMessage,
            actionTitle: L10n.loginRequiredAction,
            action: onNavigateToLogin
        )
    }
}
