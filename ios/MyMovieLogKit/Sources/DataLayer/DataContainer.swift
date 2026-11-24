import Domain
import Foundation
import OSLog
import Supabase

// live repos, the app wraps them in UseCaseContainer
public struct DataContainer: Sendable {
    public let authRepository: any AuthRepository
    public let movieRepository: any MovieRepository
    public let recordRepository: any MovieRecordRepository
    public let holidayRepository: any HolidayRepository
    public let photoRepository: any PhotoRepository

    // same Google OAuth deep link as android
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
        photoRepository = PhotoRepositoryImpl(storage: supabase.storage)
        holidayRepository = HolidayRepositoryImpl(
            api: config.publicDataApiKey.map { HolidayApi(serviceKey: $0) },
            cache: HolidayCache(fileURL: HolidayCache.defaultFileURL())
        )
        if config.publicDataApiKey == nil {
            Log.app.info("PUBLIC_DATA_API_KEY not set: holidays are not shown")
        }
    }

    // finish OAuth when opened via `mymovie://login-callback`
    public func handleOpenURL(_ url: URL) {
        supabase.auth.handle(url)
    }
}
