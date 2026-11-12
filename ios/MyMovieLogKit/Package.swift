// swift-tools-version: 6.0
import PackageDescription

// Domain → DataLayer / Presentation, the same layering as the Android app.
// The package also builds for macOS so the layers can be compiled and tested without a simulator.
let package = Package(
    name: "MyMovieLogKit",
    defaultLocalization: "ko",
    platforms: [.iOS(.v17), .macOS(.v14)],
    products: [
        .library(name: "Domain", targets: ["Domain"]),
        .library(name: "DataLayer", targets: ["DataLayer"]),
        .library(name: "Presentation", targets: ["Presentation"])
    ],
    dependencies: [
        .package(url: "https://github.com/supabase/supabase-swift.git", from: "2.30.0")
    ],
    targets: [
        .target(name: "Domain"),
        .target(
            name: "DataLayer",
            dependencies: [
                "Domain",
                .product(name: "Supabase", package: "supabase-swift")
            ]
        ),
        .target(
            name: "Presentation",
            dependencies: ["Domain"],
            resources: [.copy("Resources/Jua-Regular.ttf")]
        ),
        .testTarget(name: "DomainTests", dependencies: ["Domain"]),
        .testTarget(name: "DataLayerTests", dependencies: ["DataLayer"]),
        .testTarget(name: "PresentationTests", dependencies: ["Presentation"])
    ]
)
