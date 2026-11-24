@testable import DataLayer
import CoreGraphics
import Foundation
import ImageIO
import Testing
import UniformTypeIdentifiers

struct ImageCompressorTests {
    /// A width × height JPEG carrying an EXIF orientation
    private func jpeg(width: Int, height: Int, orientation: Int = 1) throws -> Data {
        let context = try #require(CGContext(data: nil, width: width, height: height, bitsPerComponent: 8, bytesPerRow: 0,
                                             space: CGColorSpaceCreateDeviceRGB(),
                                             bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue))
        context.setFillColor(CGColor(red: 0.98, green: 0.82, blue: 0.48, alpha: 1))
        context.fill(CGRect(x: 0, y: 0, width: width, height: height))
        let image = try #require(context.makeImage())
        let output = NSMutableData()
        let destination = try #require(CGImageDestinationCreateWithData(output, UTType.jpeg.identifier as CFString, 1, nil))
        CGImageDestinationAddImage(destination, image, [kCGImagePropertyOrientation: orientation] as CFDictionary)
        #expect(CGImageDestinationFinalize(destination))
        return output as Data
    }

    private func pixelSize(_ data: Data) throws -> (Int, Int) {
        let source = try #require(CGImageSourceCreateWithData(data as CFData, nil))
        let properties = try #require(CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any])
        return (try #require(properties[kCGImagePropertyPixelWidth] as? Int), try #require(properties[kCGImagePropertyPixelHeight] as? Int))
    }

    @Test func shrinksTheLongSideTo1920() throws {
        let output = try ImageCompressor.jpegData(from: jpeg(width: 4000, height: 3000))
        let (width, height) = try pixelSize(output)
        #expect(width == 1920)
        #expect(height == 1440)
    }

    @Test func appliesTheExifRotation() throws {
        // Orientation 6: stored landscape, shown portrait
        let output = try ImageCompressor.jpegData(from: jpeg(width: 400, height: 300, orientation: 6))
        let (width, height) = try pixelSize(output)
        #expect(width == 300)
        #expect(height == 400)
    }

    @Test func doesNotUpscaleSmallImages() throws {
        let (width, height) = try pixelSize(ImageCompressor.jpegData(from: jpeg(width: 640, height: 480)))
        #expect(width == 640)
        #expect(height == 480)
    }

    @Test func rejectsNonImageData() {
        #expect(throws: ImageCompressionError.self) {
            try ImageCompressor.jpegData(from: Data("not an image".utf8))
        }
    }
}
