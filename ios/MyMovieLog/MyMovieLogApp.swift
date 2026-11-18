import DataLayer
import Domain
import Presentation
import SwiftUI

@main
struct MyMovieLogApp: App {
    private let container: AppContainer?

    init() {
        container = AppContainer.live()
    }

    var body: some Scene {
        WindowGroup {
            if let container {
                RootView(useCases: container.useCases)
                    .onOpenURL(perform: container.data.handleOpenURL)
            } else {
                MissingConfigurationView()
            }
        }
    }
}

/// Composition root: repositories from the data layer, wrapped in use cases for the UI
struct AppContainer {
    let data: DataContainer
    let useCases: UseCaseContainer

    static func live(bundle: Bundle = .main) -> AppContainer? {
        guard let config = AppConfig.fromInfoDictionary(bundle.infoDictionary ?? [:]) else { return nil }
        let data = DataContainer(config: config)
        return AppContainer(
            data: data,
            useCases: UseCaseContainer(
                authRepository: data.authRepository,
                movieRepository: data.movieRepository,
                recordRepository: data.recordRepository
            )
        )
    }
}

private struct MissingConfigurationView: View {
    var body: some View {
        ContentUnavailableView {
            Label(ConfigurationStrings.missingKeyTitle, systemImage: "key")
        } description: {
            Text(ConfigurationStrings.missingKeyMessage)
        }
    }
}
