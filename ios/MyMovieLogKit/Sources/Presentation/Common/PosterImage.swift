import SwiftUI

/// TMDB poster in the 2:3 ratio, with a film placeholder while loading or when there is none
struct PosterImage: View {
    let url: URL?
    var cornerRadius: CGFloat = PopcornRadius.medium

    var body: some View {
        Color.clear
            .aspectRatio(2.0 / 3.0, contentMode: .fit)
            .overlay {
                AsyncImage(url: url) { phase in
                    if let image = phase.image {
                        image.resizable().scaledToFill()
                    } else {
                        placeholder
                    }
                }
            }
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius))
            .accessibilityHidden(true)
    }

    private var placeholder: some View {
        ZStack {
            Rectangle().fill(Color.popcornButterLight)
            Image(systemName: "film")
                .font(.title2)
                .foregroundStyle(Color.popcornCaramel.opacity(0.4))
        }
    }
}
