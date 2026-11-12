import SwiftUI

/// Tapping a star sets that rating; tapping the same star again makes it a half star,
/// the same interaction as the Android StarRatingRow.
struct StarRatingInput: View {
    @Binding var rating: Double

    var body: some View {
        HStack(spacing: 6) {
            ForEach(1...5, id: \.self) { star in
                let value = Double(star)
                Image(systemName: symbol(for: value))
                    .font(.title2)
                    .foregroundStyle(rating >= value - 0.5 ? Color.popcornStar : Color.popcornStarEmpty)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        rating = rating == value ? value - 0.5 : value
                    }
                    .accessibilityLabel("\(star)점")
                    .accessibilityAddTraits(rating >= value ? .isSelected : [])
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityValue(rating > 0 ? "\(rating.ratingText)점" : "없음")
    }

    private func symbol(for value: Double) -> String {
        if rating >= value { return "star.fill" }
        if rating >= value - 0.5 { return "star.leadinghalf.filled" }
        return "star"
    }
}

/// "★ 4.5" label for lists
struct RatingLabel: View {
    let rating: Double?

    var body: some View {
        if let rating {
            HStack(spacing: 3) {
                Image(systemName: "star.fill")
                    .foregroundStyle(Color.popcornStar)
                Text(rating.ratingText)
                    .foregroundStyle(Color.popcornInkMuted)
            }
            .font(.caption)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("별점 \(rating.ratingText)점")
        }
    }
}

/// Read-only five stars; [StarRatingInput] is the editable version
struct RatingStars: View {
    let rating: Double

    var body: some View {
        HStack(spacing: 2) {
            ForEach(1...5, id: \.self) { star in
                let value = Double(star)
                Image(systemName: rating >= value || rating < value - 0.5 ? "star.fill" : "star.leadinghalf.filled")
                    .foregroundStyle(rating >= value - 0.5 ? Color.popcornStar : Color.popcornStarEmpty)
            }
        }
        .font(.title3)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("별점 5점 중 \(rating.ratingText)점")
    }
}

extension Double {
    /// "4" for whole ratings, "4.5" otherwise
    var ratingText: String {
        truncatingRemainder(dividingBy: 1) == 0 ? String(format: "%.0f", self) : String(format: "%.1f", self)
    }
}
