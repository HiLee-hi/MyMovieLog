import Domain
import Foundation
import OSLog
import Supabase

/// Builds the live repositories. The app turns them into a `UseCaseContainer` at start-up.
public struct DataContainer: Sendable {
    public let authRepository: any AuthRepository
    public let movieRepository: any MovieRepository
    public let recordRepository: any MovieRecordRepository

    /// Same deep link the Android app registers for Google OAuth
    public static let authCallbackURL = URL(string: "mymovie://login-callback")!

    private let supabase: SupabaseClient

    public init(config: AppConfig) {
        Log.app.info("Supabase client init: url=\(config.supabaseURL.host() ?? "-", privacy: .public)")
        supabase = SupabaseClient(
            supabaseURL: config.supabaseURL,
            supabaseKey: config.supabaseAnonKey,
            options: SupabaseClientOptions(
                auth: .init(
                    redirectToURL: Self.authCallbackURL,
                    emitLocalSessionAsInitialSession: true
                )
            )
        )
        authRepository = AuthRepositoryImpl(auth: supabase.auth)
        movieRepository = MovieRepositoryImpl(api: TmdbApi(apiKey: config.tmdbApiKey))
        recordRepository = MovieRecordRepositoryImpl(
            remote: SupabaseMovieRecordDataSource(client: supabase),
            cache: RecordCache(fileURL: RecordCache.defaultFileURL())
        )
    }

    /// Completes the OAuth flow when the app is opened through `mymovie://login-callback`
    public func handleOpenURL(_ url: URL) {
        supabase.auth.handle(url)
    }
}
