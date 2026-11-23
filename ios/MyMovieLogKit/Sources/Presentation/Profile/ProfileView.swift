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
            getRecords: useCases.getRecords,
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
        .task { await viewModel.observeRecordCounts() }
        .onChange(of: viewModel.emailAuthState) { _, state in
            if state == .success {
                password = ""
                viewModel.resetEmailAuthState()
                onLoginSuccess()
            }
        }
    }

    /// Profile card, record counts and an outlined sign-out button, as on the design canvas
    private func signedIn(_ user: UserProfile) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                VStack(spacing: 6) {
                    avatar(user)
                        .padding(.bottom, 8)
                    Text(user.displayName ?? L10n.profileDefaultName)
                        .font(.jua(22, relativeTo: .title2))
                        .foregroundStyle(Color.popcornInk)
                    Text(user.email)
                        .font(.subheadline)
                        .foregroundStyle(Color.popcornInkMuted)
                }
                .frame(maxWidth: .infinity)
                .padding(.horizontal, 20)
                .padding(.top, 28)
                .padding(.bottom, 24)
                .popcornCard()

                HStack(spacing: 12) {
                    countCard(L10n.profileWatchedCount, count: viewModel.recordCounts.watched)
                    countCard(L10n.profileWishlistCount, count: viewModel.recordCounts.wishlist)
                }

                Button {
                    Task { await viewModel.signOut() }
                } label: {
                    Text(L10n.profileSignOut)
                        .font(.body.weight(.bold))
                        .foregroundStyle(Color.popcornInk)
                        .frame(maxWidth: .infinity, minHeight: 52)
                        .background(Color.popcornSurface, in: Capsule())
                        .overlay { Capsule().strokeBorder(Color.popcornOutlineStrong) }
                        .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .padding(.top, 12)
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 8)
            .frame(maxWidth: 560)
            .frame(maxWidth: .infinity)
        }
        .popcornScreenBackground()
    }

    private func countCard(_ label: String, count: Int) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.footnote.weight(.medium))
                .foregroundStyle(Color.popcornInkMuted)
            Text(L10n.commonMovieCount(count))
                .font(.jua(24))
                .foregroundStyle(Color.popcornInk)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .background(Color.popcornButterLight, in: RoundedRectangle(cornerRadius: PopcornRadius.large))
        .accessibilityElement(children: .combine)
    }

    /// Butter circle with the Jua initial and a butter-light ring; the Google photo when there is one
    private func avatar(_ user: UserProfile) -> some View {
        AsyncImage(url: user.avatarUrl.flatMap(URL.init(string:))) { image in
            image.resizable().scaledToFill()
        } placeholder: {
            ZStack {
                Circle().fill(Color.popcornButter)
                Text(String((user.displayName ?? user.email).prefix(1)).uppercased())
                    .font(.jua(36))
                    .foregroundStyle(Color.popcornInk)
            }
        }
        .frame(width: 88, height: 88)
        .clipShape(Circle())
        .padding(4)
        .background(Color.popcornButterLight, in: Circle())
        .accessibilityHidden(true)
    }

    private var verificationPending: some View {
        PopcornEmptyState(
            title: L10n.profileVerifyEmailTitle,
            systemImage: "envelope.badge",
            message: L10n.profileVerifyEmailMessage,
            actionTitle: L10n.profileVerifiedSignIn
        ) {
            isSignUpMode = false
            password = ""
            viewModel.resetEmailAuthState()
        }
        .popcornScreenBackground()
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
        .popcornScreenBackground()
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
