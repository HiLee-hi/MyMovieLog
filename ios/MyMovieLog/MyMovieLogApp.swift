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
            Label("API 키가 설정되지 않았어요", systemImage: "key")
        } description: {
            Text("ios/scripts/generate_secrets.sh 를 실행해 Config/Secrets.xcconfig 를 만든 뒤 다시 빌드해주세요.")
        }
    }
}
