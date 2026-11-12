import SwiftUI

struct LoginRequiredView: View {
    var message = "로그인이 필요한 기능이에요"
    let onNavigateToLogin: () -> Void

    var body: some View {
        ContentUnavailableView {
            Label(message, systemImage: "lock")
        } description: {
            Text("감상 기록을 저장하고 확인하려면\n로그인해주세요.")
        } actions: {
            Button("로그인하기", action: onNavigateToLogin)
                .buttonStyle(.borderedProminent)
        }
    }
}
