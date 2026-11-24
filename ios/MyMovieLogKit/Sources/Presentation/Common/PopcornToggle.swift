import SwiftUI

// pill options in a white track, selected = butter
// replaces segmented Picker (library tabs, watched / wishlist), same as android PopcornToggle
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
