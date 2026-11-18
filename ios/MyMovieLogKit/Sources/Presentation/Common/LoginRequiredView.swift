import SwiftUI

struct LoginRequiredView: View {
    var message = L10n.loginRequiredTitle
    let onNavigateToLogin: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label(message, systemImage: "lock")
        } description: {
            Text(L10n.loginRequiredMessage)
        } actions: {
            Button(L10n.loginRequiredAction, action: onNavigateToLogin)
                .buttonStyle(.borderedProminent)
        }
    }
}
