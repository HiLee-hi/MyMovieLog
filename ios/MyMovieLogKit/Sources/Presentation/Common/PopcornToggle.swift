import SwiftUI

/// Pill options inside a white rounded track; the selected one is filled with butter.
/// Replaces the segmented Picker for the library tabs and the watched / wishlist choice,
/// the same control as the Android `PopcornToggle`.
struct PopcornToggle<Value: Hashable>: View {
    let options: [Value]
    @Binding var selection: Value
    let label: (Value) -> String

    var body: some View {
        HStack(spacing: 4) {
            ForEach(options, id: \.self) { option in
                let isSelected = option == selection
                Button {
                    selection = option
                } label: {
                    Text(label(option))
                        .font(.subheadline.weight(isSelected ? .bold : .medium))
                        .foregroundStyle(isSelected ? Color.popcornInk : Color.popcornInkMuted)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                        .frame(maxWidth: .infinity, minHeight: 40)
                        .background(isSelected ? Color.popcornButter : Color.clear, in: Capsule())
                        .contentShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityAddTraits(isSelected ? .isSelected : [])
            }
        }
        .padding(4)
        .background(Color.popcornSurface, in: Capsule())
        .overlay(Capsule().strokeBorder(Color.popcornOutline))
        .animation(.easeOut(duration: 0.15), value: selection)
    }
}
