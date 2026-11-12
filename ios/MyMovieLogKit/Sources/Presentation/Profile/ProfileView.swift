import Domain
import SwiftUI

struct ProfileView: View {
    private let onLoginSuccess: () -> Void
    @State private var viewModel: ProfileViewModel
    @State private var email = ""
    @State private var password = ""
    @State private var isSignUpMode = false

    init(useCases: UseCaseContainer, onLoginSuccess: @escaping () -> Void) {
        self.onLoginSuccess = onLoginSuccess
        _viewModel = State(initialValue: ProfileViewModel(
            observeCurrentUser: useCases.observeCurrentUser,
            signInWithEmail: useCases.signInWithEmail,
            signUpWithEmail: useCases.signUpWithEmail,
            signInWithGoogle: useCases.signInWithGoogle,
            signOut: useCases.signOut
        ))
    }

    var body: some View {
        NavigationStack {
            Group {
                if let user = viewModel.currentUser {
                    signedIn(user)
                } else if viewModel.emailAuthState == .emailVerificationRequired {
                    verificationPending
                } else {
                    loginForm
                }
            }
            .navigationTitle("프로필")
        }
        .task { await viewModel.observeUser() }
        .onChange(of: viewModel.emailAuthState) { _, state in
            if state == .success {
                password = ""
                viewModel.resetEmailAuthState()
                onLoginSuccess()
            }
        }
    }

    private func signedIn(_ user: UserProfile) -> some View {
        List {
            Section {
                HStack(spacing: 16) {
                    avatar(user)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(user.displayName ?? "팝콘한알 사용자")
                            .font(.headline)
                        Text(user.email)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                .padding(.vertical, 6)
            }
            Section {
                Button("로그아웃", role: .destructive) {
                    Task { await viewModel.signOut() }
                }
            }
        }
        .groupedListStyle()
    }

    private func avatar(_ user: UserProfile) -> some View {
        AsyncImage(url: user.avatarUrl.flatMap(URL.init(string:))) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            ZStack {
                Circle().fill(Color.accentColor.opacity(0.2))
                Text(String((user.displayName ?? user.email).prefix(1)).uppercased())
                    .font(.title2.bold())
                    .foregroundStyle(Color.accentColor)
            }
        }
        .frame(width: 56, height: 56)
        .clipShape(Circle())
        .accessibilityHidden(true)
    }

    private var verificationPending: some View {
        ContentUnavailableView {
            Label("이메일을 확인해주세요", systemImage: "envelope.badge")
        } description: {
            Text("가입하신 이메일로 인증 링크를 발송했습니다.\n링크를 클릭한 후 로그인해주세요.")
        } actions: {
            Button("인증 완료 — 로그인하기") {
                isSignUpMode = false
                password = ""
                viewModel.resetEmailAuthState()
            }
            .buttonStyle(.borderedProminent)
        }
    }

    private var loginForm: some View {
        let isLoading = viewModel.emailAuthState == .loading
        return Form {
            Section {
                VStack(alignment: .leading, spacing: 6) {
                    Text("로그인하여 기록을 저장하세요")
                        .font(.headline)
                    Text("Android 앱과 같은 계정으로 로그인하면 기록이 함께 보여요")
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }

            Section(isSignUpMode ? "회원가입" : "이메일로 로그인") {
                TextField("이메일", text: $email)
                    .emailInput()
                SecureField("비밀번호 (6자 이상)", text: $password)
                    .passwordInput(isNew: isSignUpMode)
                if case .error(let message) = viewModel.emailAuthState {
                    Text(message)
                        .font(.footnote)
                        .foregroundStyle(.red)
                }
            }

            // Buttons sit on the grouped background instead of inside rows: a centered button
            // row gets a separator that starts at its text, which looks like a stray line.
            Section {
                Button {
                    Task {
                        if isSignUpMode {
                            await viewModel.signUpWithEmail(email: email, password: password)
                        } else {
                            await viewModel.signInWithEmail(email: email, password: password)
                        }
                    }
                } label: {
                    Group {
                        if isLoading {
                            ProgressView()
                        } else {
                            Text(isSignUpMode ? "회원가입" : "로그인").fontWeight(.semibold)
                        }
                    }
                }
                .buttonStyle(.popcornPrimary)
                .disabled(isLoading || !email.contains("@") || password.count < 6)
                .buttonRow()

                Button(isSignUpMode ? "이미 계정이 있어요 — 로그인" : "계정이 없으신가요? 회원가입") {
                    isSignUpMode.toggle()
                    viewModel.resetEmailAuthState()
                }
                .font(.footnote)
                .frame(maxWidth: .infinity)
                .buttonStyle(.borderless)
                .buttonRow()
            }

            Section {
                Button {
                    Task {
                        if await viewModel.signInWithGoogle() { onLoginSuccess() }
                    }
                } label: {
                    Label("Google로 로그인", systemImage: "g.circle")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.bordered)
                .controlSize(.large)
                .buttonRow()

                if let message = viewModel.googleError {
                    Text(message)
                        .font(.footnote)
                        .foregroundStyle(.red)
                        .buttonRow()
                }
            }
        }
        .disabled(isLoading)
    }
}

private extension View {
    /// A Form row that shows only its content: no row background, inset or separator
    func buttonRow() -> some View {
        listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets())
            .listRowSeparator(.hidden)
    }
}
