import SwiftUI

// empty / error / "pick something" states in butter cream, replaces ContentUnavailableView
struct PopcornEmptyState: View {
    let title: String
    var systemImage: String = "film"
    var message: String?
    var actionTitle: String?
    var action: (() -> Void)?

    var body: some View {
        VStack(spacing: 14) {
            Image(systemName: systemImage)
                .font(.system(size: 30, weight: .medium))
                .foregroundStyle(Color.popcornCaramel)
                .frame(width: 72, height: 72)
                .background(Color.popcornButterLight, in: Circle())
                .accessibilityHidden(true)
            Text(title)
                .font(.jua(20, relativeTo: .title3))
                .foregroundStyle(Color.popcornInk)
                .multilineTextAlignment(.center)
            if let message {
                Text(message)
                    .font(.subheadline)
                    .foregroundStyle(Color.popcornInkMuted)
                    .multilineTextAlignment(.center)
            }
            if let actionTitle, let action {
                Button(actionTitle, action: action)
                    .buttonStyle(.popcornPrimary)
                    .frame(maxWidth: 240)
                    .padding(.top, 4)
            }
        }
        .padding(32)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .accessibilityElement(children: .contain)
    }
}

// white capsule search field, butter outline, clear button
struct PopcornSearchField: View {
    @Binding var text: String
    let prompt: String
    @FocusState private var isFocused: Bool

    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: "magnifyingglass")
                .font(.system(size: 18, weight: .medium))
                .foregroundStyle(Color.popcornInkMuted)
                .accessibilityHidden(true)
            TextField(prompt, text: $text, prompt: Text(prompt).foregroundStyle(Color(hex: 0xA8957F)))
                .focused($isFocused)
                .submitLabel(.search)
                .autocorrectionDisabled()
                .foregroundStyle(Color.popcornInk)
            if !text.isEmpty {
                Button {
                    text = ""
                    isFocused = true
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(Color.popcornInk)
                        .frame(width: 36, height: 36)
                        .background(Color.popcornButterLight, in: Circle())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(L10n.searchClear)
            }
        }
        .padding(.leading, 16)
        .padding(.trailing, 6)
        .frame(minHeight: 52)
        .background(Color.popcornSurface, in: Capsule())
        .overlay { Capsule().strokeBorder(Color.popcornButter, lineWidth: 1.5) }
        .shadow(color: .popcornShadow, radius: 8, y: 4)
        .contentShape(Capsule())
        .onTapGesture { isFocused = true }
    }
}
