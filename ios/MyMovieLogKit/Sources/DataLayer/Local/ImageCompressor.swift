import Foundation
import ImageIO
import UniformTypeIdentifiers

enum ImageCompressionError: Error {
    case unreadableImage
    case encodingFailed
}

// same as android compressImage: long side <= 1920 px, EXIF rotation baked in, JPEG 0.85
// JPEG / HEIC / PNG input
enum ImageCompressor {
    static let maxDimension = 1920
    static let jpegQuality = 0.85

    static func jpegData(from data: Data) throws -> Data {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil),
              CGImageSourceGetCount(source) > 0
        else { throw ImageCompressionError.unreadableImage }

        // thumbnail with transform = downscale + upright in one step
        // "Always" -> decode the full image, not the embedded preview
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true,
            kCGImageSourceThumbnailMaxPixelSize: min(maxDimension, longSide(of: source) ?? maxDimension),
        ]
        guard let image = CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary)
        else { throw ImageCompressionError.unreadableImage }

        let output = NSMutableData()
        guard let destination = CGImageDestinationCreateWithData(output, UTType.jpeg.identifier as CFString, 1, nil)
        else { throw ImageCompressionError.encodingFailed }
        CGImageDestinationAddImage(destination, image, [kCGImageDestinationLossyCompressionQuality: jpegQuality] as CFDictionary)
        guard CGImageDestinationFinalize(destination) else { throw ImageCompressionError.encodingFailed }
        return output as Data
    }

    // no upscaling
    private static func longSide(of source: CGImageSource) -> Int? {
        guard let properties = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
              let width = properties[kCGImagePropertyPixelWidth] as? Int,
              let height = properties[kCGImagePropertyPixelHeight] as? Int
        else { return nil }
        return max(width, height)
    }
}
