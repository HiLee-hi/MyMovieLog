import CoreText
import SwiftUI
#if os(iOS)
import UIKit
#endif

/// Butter cream palette, the same tokens as the Android PopcornColors.
/// Butter is too light for text, so text accents use caramel.
extension Color {
    static let popcornCream = Color(hex: 0xFFFCF5)
    static let popcornSurface = Color.white
    static let popcornButterLight = Color(hex: 0xFFF5DC)
    static let popcornButter = Color(hex: 0xFAD27A)
    static let popcornCaramel = Color(hex: 0x9A5214)
    static let popcornInk = Color(hex: 0x3B2A1A)
    static let popcornInkMuted = Color(hex: 0x7A6652)
    static let popcornOutline = Color(hex: 0xF1E6D2)
    static let popcornOutlineStrong = Color(hex: 0xCDB99A)
    static let popcornStar = Color(hex: 0xF2A516)
    static let popcornStarEmpty = Color(hex: 0xE8DAC2)
    static let popcornShadow = Color(hex: 0x9A5214).opacity(0.12)

    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

extension Font {
    /// Rounded Jua display face for titles. Scales with Dynamic Type relative to [textStyle].
    static func jua(_ size: CGFloat, relativeTo textStyle: Font.TextStyle = .title) -> Font {
        .custom(PopcornTheme.juaName, size: size, relativeTo: textStyle)
    }
}

enum PopcornRadius {
    static let small: CGFloat = 8
    static let medium: CGFloat = 14
    static let large: CGFloat = 20
}

public enum PopcornTheme {
    static let juaName = "Jua-Regular"

    /// Registers the bundled font and styles the UIKit bars. Call once before the first view.
    @MainActor
    public static func install() {
        registerFonts()
        #if os(iOS)
        applyBarAppearance()
        #endif
    }

    private static func registerFonts() {
        guard let url = Bundle.module.url(forResource: juaName, withExtension: "ttf") else { return }
        // Registering twice (tests, previews) reports an error that is safe to ignore
        CTFontManagerRegisterFontsForURL(url as CFURL, .process, nil)
    }

    #if os(iOS)
    @MainActor
    private static func applyBarAppearance() {
        let ink = UIColor(Color.popcornInk)
        let navigation = UINavigationBarAppearance()
        navigation.configureWithOpaqueBackground()
        navigation.backgroundColor = UIColor(Color.popcornCream)
        navigation.shadowColor = .clear
        if let large = UIFont(name: juaName, size: 30), let inline = UIFont(name: juaName, size: 19) {
            navigation.largeTitleTextAttributes = [.font: UIFontMetrics(forTextStyle: .largeTitle).scaledFont(for: large), .foregroundColor: ink]
            navigation.titleTextAttributes = [.font: UIFontMetrics(forTextStyle: .headline).scaledFont(for: inline), .foregroundColor: ink]
        }
        UINavigationBar.appearance().standardAppearance = navigation
        UINavigationBar.appearance().compactAppearance = navigation
        UINavigationBar.appearance().scrollEdgeAppearance = navigation

        let tab = UITabBarAppearance()
        tab.configureWithOpaqueBackground()
        tab.backgroundColor = .white
        tab.shadowColor = UIColor(Color.popcornOutline)
        UITabBar.appearance().standardAppearance = tab
        UITabBar.appearance().scrollEdgeAppearance = tab
    }
    #endif
}

/// Butter filled button with ink text, the main call to action.
struct PopcornPrimaryButtonStyle: ButtonStyle {
    @Environment(\.isEnabled) private var isEnabled

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(.body.weight(.bold))
            .foregroundStyle(Color.popcornInk)
            .frame(maxWidth: .infinity, minHeight: 52)
            .background(Color.popcornButter, in: Capsule())
            .shadow(color: .popcornShadow, radius: 8, y: 4)
            .opacity(isEnabled ? (configuration.isPressed ? 0.8 : 1) : 0.5)
    }
}

extension ButtonStyle where Self == PopcornPrimaryButtonStyle {
    static var popcornPrimary: PopcornPrimaryButtonStyle { PopcornPrimaryButtonStyle() }
}

extension View {
    /// White card with a thin outline, the default container of the design
    func popcornCard(fill: Color = .popcornSurface, outlined: Bool = true) -> some View {
        background(fill, in: RoundedRectangle(cornerRadius: PopcornRadius.large))
            .overlay {
                if outlined {
                    RoundedRectangle(cornerRadius: PopcornRadius.large)
                        .strokeBorder(Color.popcornOutline)
                }
            }
    }

    /// Cream screen background behind lists and scroll views
    func popcornScreenBackground() -> some View {
        scrollContentBackground(.hidden)
            .background(Color.popcornCream)
    }
}

/// Brand mark: four popcorn puffs around a butter center
struct PopcornMark: View {
    var size: CGFloat = 34

    var body: some View {
        Canvas { context, canvasSize in
            let scale = canvasSize.width / 32
            let puffs: [(CGFloat, CGFloat, CGFloat)] = [(11, 13, 6), (20, 11, 6.5), (21, 20.5, 6), (11.5, 21, 6)]
            for (x, y, r) in puffs {
                let rect = CGRect(x: (x - r) * scale, y: (y - r) * scale, width: 2 * r * scale, height: 2 * r * scale)
                let circle = Path(ellipseIn: rect)
                context.fill(circle, with: .color(Color(hex: 0xFFF6E0)))
                context.stroke(circle, with: .color(.popcornCaramel), lineWidth: 1.6 * scale)
            }
            let center = CGRect(x: 12.8 * scale, y: 13.3 * scale, width: 6.4 * scale, height: 6.4 * scale)
            context.fill(Path(ellipseIn: center), with: .color(.popcornButter))
        }
        .frame(width: size, height: size)
        .accessibilityHidden(true)
    }
}
