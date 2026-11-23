/// Every use case the presentation layer needs, built from the repositories once at app start.
/// ViewModels receive individual use cases from here, never a repository.
public struct UseCaseContainer: Sendable {
    public let observeCurrentUser: ObserveCurrentUserUseCase
    public let getCurrentUserId: GetCurrentUserIdUseCase
    public let signInWithEmail: SignInWithEmailUseCase
    public let signUpWithEmail: SignUpWithEmailUseCase
    public let signInWithGoogle: SignInWithGoogleUseCase
    public let signOut: SignOutUseCase

    public let searchMovies: SearchMoviesUseCase
    public let getMovieDetail: GetMovieDetailUseCase

    public let getRecords: GetRecordsUseCase
    public let getRecordByTmdbId: GetRecordByTmdbIdUseCase
    public let getRecordsByDate: GetRecordsByDateUseCase
    public let getWatchedDatesByMonth: GetWatchedDatesByMonthUseCase
    public let upsertRecord: UpsertRecordUseCase
    public let deleteRecord: DeleteRecordUseCase
    public let syncFromRemote: SyncFromRemoteUseCase
    public let clearLocalCache: ClearLocalCacheUseCase

    public let getHolidaysByMonth: GetHolidaysByMonthUseCase
    public let getSignedPhotoUrls: GetSignedPhotoUrlsUseCase

    public init(
        authRepository: any AuthRepository,
        movieRepository: any MovieRepository,
        recordRepository: any MovieRecordRepository,
        holidayRepository: any HolidayRepository,
        photoRepository: any PhotoRepository
    ) {
        observeCurrentUser = ObserveCurrentUserUseCase(repository: authRepository)
        getCurrentUserId = GetCurrentUserIdUseCase(repository: authRepository)
        signInWithEmail = SignInWithEmailUseCase(repository: authRepository)
        signUpWithEmail = SignUpWithEmailUseCase(repository: authRepository)
        signInWithGoogle = SignInWithGoogleUseCase(repository: authRepository)
        signOut = SignOutUseCase(repository: authRepository)

        searchMovies = SearchMoviesUseCase(repository: movieRepository)
        getMovieDetail = GetMovieDetailUseCase(repository: movieRepository)

        getRecords = GetRecordsUseCase(repository: recordRepository)
        getRecordByTmdbId = GetRecordByTmdbIdUseCase(repository: recordRepository)
        getRecordsByDate = GetRecordsByDateUseCase(repository: recordRepository)
        getWatchedDatesByMonth = GetWatchedDatesByMonthUseCase(repository: recordRepository)
        upsertRecord = UpsertRecordUseCase(repository: recordRepository, photoRepository: photoRepository)
        deleteRecord = DeleteRecordUseCase(repository: recordRepository, photoRepository: photoRepository)
        syncFromRemote = SyncFromRemoteUseCase(repository: recordRepository)
        clearLocalCache = ClearLocalCacheUseCase(repository: recordRepository)

        getHolidaysByMonth = GetHolidaysByMonthUseCase(repository: holidayRepository)
        getSignedPhotoUrls = GetSignedPhotoUrlsUseCase(repository: photoRepository)
    }
}
