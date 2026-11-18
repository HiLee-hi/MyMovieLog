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
            .navigationTitle(L10n.tabProfile)
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
                        Text(user.displayName ?? L10n.profileDefaultName)
                            .font(.headline)
                        Text(user.email)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
                .padding(.vertical, 6)
            }
            Section {
                Button(L10n.profileSignOut, role: .destructive) {
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
            Label(L10n.profileVerifyEmailTitle, systemImage: "envelope.badge")
        } description: {
            Text(L10n.profileVerifyEmailMessage)
        } actions: {
            Button(L10n.profileVerifiedSignIn) {
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
                    Text(L10n.profileSignInPrompt)
                        .font(.headline)
                    Text(L10n.profileSharedAccountHint)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
            }

            Section(isSignUpMode ? L10n.profileSignUp : L10n.profileSignInEmail) {
                TextField(L10n.profileEmail, text: $email)
                    .emailInput()
                SecureField(L10n.profilePassword, text: $password)
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
                            Text(isSignUpMode ? L10n.profileSignUp : L10n.commonSignIn).fontWeight(.semibold)
                        }
                    }
                }
                .buttonStyle(.popcornPrimary)
                .disabled(isLoading || !email.contains("@") || password.count < 6)
                .buttonRow()

                Button(isSignUpMode ? L10n.profileHaveAccount : L10n.profileNoAccount) {
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
                    Label(L10n.profileSignInGoogle, systemImage: "g.circle")
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
