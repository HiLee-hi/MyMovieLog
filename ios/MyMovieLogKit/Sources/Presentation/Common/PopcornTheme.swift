import CoreText
import SwiftUI
#if os(iOS)
import UIKit
#endif

// same tokens as android PopcornColors, butter too light for text -> caramel accents
extension Color {
    static let popcornCream = Color(hex: 0xFFFCF5)
    static let popcornSurface = Color.white
    static let popcornButterLight = Color(hex: 0xFFF5DC)
    static let popcornButter = Color(hex: 0xFAD27A)
    // past months in the stats chart, a step lighter than butter
    static let popcornButterSoft = Color(hex: 0xF6DDA8)
    static let popcornCaramel = Color(hex: 0x9A5214)
    static let popcornInk = Color(hex: 0x3B2A1A)
    static let popcornInkMuted = Color(hex: 0x7A6652)
    static let popcornOutline = Color(hex: 0xF1E6D2)
    static let popcornOutlineStrong = Color(hex: 0xCDB99A)
    static let popcornStar = Color(hex: 0xF2A516)
    static let popcornStarEmpty = Color(hex: 0xE8DAC2)
    // sundays / holidays, android M3 error red
    static let popcornHoliday = Color(hex: 0xB3261E)
    // saturdays, android PopcornColors.Saturday
    static let popcornSaturday = Color(hex: 0x1976D2)
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
    // Jua for titles, scales with Dynamic Type relative to textStyle
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

    // register font + style UIKit bars, call once before the first view
    @MainActor
    public static func install() {
        registerFonts()
        #if os(iOS)
        applyBarAppearance()
        #endif
    }

    private static func registerFonts() {
        guard let url = Bundle.module.url(forResource: juaName, withExtension: "ttf") else { return }
        // double registration (tests, previews) errors, safe to ignore
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
        // unselected tabs in muted ink, selected follows caramel tint
        let muted = UIColor(Color.popcornInkMuted)
        for layout in [tab.stackedLayoutAppearance, tab.inlineLayoutAppearance, tab.compactInlineLayoutAppearance] {
            layout.normal.iconColor = muted
            layout.normal.titleTextAttributes = [.foregroundColor: muted]
        }
        UITabBar.appearance().standardAppearance = tab
        UITabBar.appearance().scrollEdgeAppearance = tab
    }
    #endif
}

// main CTA: butter fill, ink text
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
    // white card, thin outline (design default)
    func popcornCard(fill: Color = .popcornSurface, outlined: Bool = true) -> some View {
        background(fill, in: RoundedRectangle(cornerRadius: PopcornRadius.large))
            .overlay {
                if outlined {
                    RoundedRectangle(cornerRadius: PopcornRadius.large)
                        .strokeBorder(Color.popcornOutline)
                }
            }
    }

    // cream background behind lists / scroll views
    func popcornScreenBackground() -> some View {
        scrollContentBackground(.hidden)
            .background(Color.popcornCream)
    }
}

// brand mark: four puffs around a butter center
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
