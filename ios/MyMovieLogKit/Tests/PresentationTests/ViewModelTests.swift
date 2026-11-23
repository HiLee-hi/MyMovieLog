import Domain
import Foundation
@testable import Presentation
import Testing

/// Polls until `condition` holds; ViewModels update from streams on the main actor
@MainActor
private func eventually(_ condition: () -> Bool, timeout: Duration = .seconds(2)) async -> Bool {
    let deadline = ContinuousClock.now + timeout
    while !condition() {
        if ContinuousClock.now > deadline { return false }
        try? await Task.sleep(for: .milliseconds(10))
    }
    return true
}

private func movies(_ count: Int) -> [Movie] {
    (1...count).map { index in
        Movie(id: index, title: "Movie \(index)", originalTitle: "", overview: "", posterPath: nil,
              backdropPath: nil, releaseDate: "", voteAverage: 0, genreIds: [])
    }
}

@MainActor
struct SearchViewModelTests {
    @Test func loadsFirstPageThenMore() async {
        let repository = InMemoryMovieRepository(movies: movies(45), pageSize: 20)
        let viewModel = SearchViewModel(searchMovies: SearchMoviesUseCase(repository: repository))

        viewModel.query = "Movie"
        await viewModel.search()
        #expect(viewModel.phase == .loaded)
        #expect(viewModel.movies.count == 20)

        await viewModel.loadMoreIfNeeded(after: viewModel.movies[5])
        #expect(viewModel.movies.count == 20, "only the last row triggers paging")

        await viewModel.loadMoreIfNeeded(after: viewModel.movies.last!)
        await viewModel.loadMoreIfNeeded(after: viewModel.movies.last!)
        #expect(viewModel.movies.count == 45)
        #expect(Set(viewModel.movies.map(\.id)).count == 45)

        await viewModel.loadMoreIfNeeded(after: viewModel.movies.last!)
        #expect(repository.requestedPages == [1, 2, 3])
    }

    @Test func blankQueryResetsAndEmptyResultIsReported() async {
        let viewModel = SearchViewModel(searchMovies: SearchMoviesUseCase(repository: InMemoryMovieRepository()))
        viewModel.query = "없는 영화"
        await viewModel.search()
        #expect(viewModel.phase == .empty)

        viewModel.query = "   "
        await viewModel.search()
        #expect(viewModel.phase == .idle)
        #expect(viewModel.movies.isEmpty)
    }

    @Test func sameQueryIsNotSearchedTwice() async {
        let repository = InMemoryMovieRepository()
        let viewModel = SearchViewModel(searchMovies: SearchMoviesUseCase(repository: repository))
        viewModel.query = "기생충"
        await viewModel.search()
        await viewModel.search()
        #expect(repository.requestedPages == [1])
    }
}

@MainActor
struct MovieDetailViewModelTests {
    private func makeViewModel(
        movieId: Int,
        records: InMemoryRecordRepository,
        auth: InMemoryAuthRepository = InMemoryAuthRepository()
    ) -> MovieDetailViewModel {
        MovieDetailViewModel(
            movieId: movieId,
            getMovieDetail: GetMovieDetailUseCase(repository: InMemoryMovieRepository()),
            getRecordByTmdbId: GetRecordByTmdbIdUseCase(repository: records),
            upsertRecord: UpsertRecordUseCase(repository: records),
            getCurrentUserId: GetCurrentUserIdUseCase(repository: auth)
        )
    }

    @Test func savesNewRecordFromMovie() async {
        let records = InMemoryRecordRepository(records: [])
        let viewModel = makeViewModel(movieId: 157336, records: records)
        await viewModel.loadDetail()
        viewModel.openEditor()
        viewModel.draft.rating = 5
        viewModel.draft.review = "명작"
        await viewModel.saveRecord()

        #expect(viewModel.saveState == .success)
        #expect(viewModel.isEditorPresented == false)
        let saved = records.allRecords.first
        #expect(saved?.title == "인터스텔라")
        #expect(saved?.userId == UserProfile.preview.id)
        #expect(saved?.genreIds == [12, 18, 878])
        #expect(saved?.rating == 5)
    }

    @Test func editingKeepsIdAndAndroidPhotos() async {
        let existing = MovieRecord(id: "r9", userId: "preview-user", tmdbId: 496243, title: "기생충",
                                   status: .wishlist, memo: "보기", photoUrls: ["u/496243/a.jpg"],
                                   createdAt: "2026-01-01T00:00:00Z")
        let records = InMemoryRecordRepository(records: [existing])
        let viewModel = makeViewModel(movieId: 496243, records: records)
        await viewModel.loadDetail()
        let observer = Task { await viewModel.observeRecord() }
        defer { observer.cancel() }
        #expect(await eventually { viewModel.existingRecord != nil })

        viewModel.openEditor()
        #expect(viewModel.draft.status == .wishlist)
        #expect(viewModel.draft.memo == "보기")
        viewModel.draft.status = .watched
        await viewModel.saveRecord()

        let saved = records.allRecords
        #expect(saved.count == 1)
        #expect(saved.first?.id == "r9")
        #expect(saved.first?.status == .watched)
        #expect(saved.first?.memo == nil)
        #expect(saved.first?.photoUrls == ["u/496243/a.jpg"])
        #expect(saved.first?.createdAt == "2026-01-01T00:00:00Z")
    }

    @Test func saveFailsWhenSignedOut() async {
        let records = InMemoryRecordRepository(records: [])
        let viewModel = makeViewModel(movieId: 157336, records: records, auth: InMemoryAuthRepository(user: nil))
        await viewModel.loadDetail()
        viewModel.openEditor()
        await viewModel.saveRecord()

        #expect(viewModel.saveState == .error(DomainError.notSignedIn.localizedDescription))
        #expect(viewModel.isEditorPresented)
        #expect(records.allRecords.isEmpty)
    }

    @Test func reportsLoadFailure() async {
        let viewModel = makeViewModel(movieId: -1, records: InMemoryRecordRepository())
        await viewModel.loadDetail()
        guard case .error = viewModel.phase else {
            Issue.record("expected error, got \(viewModel.phase)")
            return
        }
    }
}

@MainActor
struct AppViewModelTests {
    @Test func syncsAfterSignInAndClearsAfterSignOut() async throws {
        let auth = InMemoryAuthRepository(user: nil)
        let records = InMemoryRecordRepository()
        let viewModel = AppViewModel(
            observeCurrentUser: ObserveCurrentUserUseCase(repository: auth),
            syncFromRemote: SyncFromRemoteUseCase(repository: records),
            clearLocalCache: ClearLocalCacheUseCase(repository: records)
        )
        let observer = Task { await viewModel.observeAuthAndSync() }
        defer { observer.cancel() }

        #expect(await eventually { viewModel.isSessionResolved })
        #expect(viewModel.isLoggedIn == false)
        #expect(await eventually { records.clearCount == 1 })

        try await auth.signInWithGoogle()
        #expect(await eventually { viewModel.isLoggedIn })
        #expect(await eventually { records.syncCount == 1 })

        try await auth.signOut()
        #expect(await eventually { !viewModel.isLoggedIn })
        #expect(await eventually { records.clearCount == 2 })
        #expect(records.syncCount == 1)
    }
}

@MainActor
struct LibraryViewModelTests {
    @Test func splitsByStatusAndDeletes() async {
        let records = InMemoryRecordRepository()
        let viewModel = LibraryViewModel(
            getRecords: GetRecordsUseCase(repository: records),
            upsertRecord: UpsertRecordUseCase(repository: records),
            deleteRecord: DeleteRecordUseCase(repository: records)
        )
        let observer = Task { await viewModel.observeRecords() }
        defer { observer.cancel() }

        #expect(await eventually { viewModel.watchedRecords.count == 2 })
        #expect(viewModel.wishlistRecords.count == 1)

        let target = viewModel.watchedRecords[0]
        viewModel.editSession.select(target)
        await viewModel.deleteRecord(target)
        #expect(await eventually { viewModel.watchedRecords.count == 1 })
        #expect(viewModel.editSession.record == nil)
    }

    @Test func editSessionSavesDraft() async {
        let records = InMemoryRecordRepository()
        let session = RecordEditSession(upsertRecord: UpsertRecordUseCase(repository: records))
        session.select(MovieRecord.previewList[2])
        #expect(session.draft.status == .wishlist)
        session.draft.memo = "이번 주말"
        await session.save()

        #expect(session.saveState == .success)
        #expect(records.allRecords.first { $0.id == "r3" }?.memo == "이번 주말")
    }
}

@MainActor
struct CalendarViewModelTests {
    @Test func loadsHolidaysOfTheShownMonth() async {
        let chuseok = LocalDate(year: 2026, month: 9, day: 25)
        let records = InMemoryRecordRepository(records: [])
        let viewModel = CalendarViewModel(
            getWatchedDatesByMonth: GetWatchedDatesByMonthUseCase(repository: records),
            getRecordsByDate: GetRecordsByDateUseCase(repository: records),
            getHolidaysByMonth: GetHolidaysByMonthUseCase(repository: InMemoryHolidayRepository(dates: [chuseok])),
            upsertRecord: UpsertRecordUseCase(repository: records),
            initialMonth: YearMonth(year: 2026, month: 9)
        )
        await viewModel.loadHolidays()
        #expect(viewModel.holidays == [chuseok])

        viewModel.showNextMonth()
        await viewModel.loadHolidays()
        #expect(viewModel.holidays.isEmpty)
    }

    @Test func followsMonthAndSelectedDate() async {
        let today = LocalDate.today()
        let records = InMemoryRecordRepository()
        let viewModel = CalendarViewModel(
            getWatchedDatesByMonth: GetWatchedDatesByMonthUseCase(repository: records),
            getRecordsByDate: GetRecordsByDateUseCase(repository: records),
            getHolidaysByMonth: GetHolidaysByMonthUseCase(repository: InMemoryHolidayRepository()),
            upsertRecord: UpsertRecordUseCase(repository: records),
            initialMonth: today.yearMonth
        )
        let datesObserver = Task { await viewModel.observeWatchedDates() }
        defer { datesObserver.cancel() }
        #expect(await eventually { viewModel.watchedDates.contains(today) })

        viewModel.selectDate(today)
        let recordsObserver = Task { await viewModel.observeSelectedDateRecords() }
        defer { recordsObserver.cancel() }
        let expected = MovieRecord.previewList.filter { $0.status == .watched && $0.watchedAt == today }.map(\.id)
        #expect(await eventually { viewModel.selectedDateRecords.map(\.id) == expected })

        viewModel.showNextMonth()
        #expect(viewModel.currentMonth == today.yearMonth.adding(months: 1))
        viewModel.dismissDate()
        #expect(viewModel.selectedDate == nil)
    }
}

@MainActor
struct ProfileViewModelTests {
    @Test func emailSignInAndSignUpStates() async {
        let auth = InMemoryAuthRepository(user: nil)
        let viewModel = ProfileViewModel(
            observeCurrentUser: ObserveCurrentUserUseCase(repository: auth),
            getRecords: GetRecordsUseCase(repository: InMemoryRecordRepository(records: [])),
            signInWithEmail: SignInWithEmailUseCase(repository: auth),
            signUpWithEmail: SignUpWithEmailUseCase(repository: auth),
            signInWithGoogle: SignInWithGoogleUseCase(repository: auth),
            signOut: SignOutUseCase(repository: auth)
        )
        let observer = Task { await viewModel.observeUser() }
        defer { observer.cancel() }

        await viewModel.signUpWithEmail(email: "a@b.c", password: "123456")
        #expect(viewModel.emailAuthState == .emailVerificationRequired)

        await viewModel.signInWithEmail(email: "a@b.c", password: "123456")
        #expect(viewModel.emailAuthState == .success)
        #expect(await eventually { viewModel.currentUser?.email == "a@b.c" })

        auth.signInError = URLError(.notConnectedToInternet)
        await viewModel.signInWithEmail(email: "a@b.c", password: "123456")
        guard case .error = viewModel.emailAuthState else {
            Issue.record("expected error state")
            return
        }
    }

    @Test func countsWatchedAndWishlistRecords() async {
        let auth = InMemoryAuthRepository()
        let records = InMemoryRecordRepository(records: [
            MovieRecord(id: "r1", userId: "preview-user", tmdbId: 1, title: "A", status: .watched),
            MovieRecord(id: "r2", userId: "preview-user", tmdbId: 2, title: "B", status: .watched),
            MovieRecord(id: "r3", userId: "preview-user", tmdbId: 3, title: "C", status: .wishlist),
        ])
        let viewModel = ProfileViewModel(
            observeCurrentUser: ObserveCurrentUserUseCase(repository: auth),
            getRecords: GetRecordsUseCase(repository: records),
            signInWithEmail: SignInWithEmailUseCase(repository: auth),
            signUpWithEmail: SignUpWithEmailUseCase(repository: auth),
            signInWithGoogle: SignInWithGoogleUseCase(repository: auth),
            signOut: SignOutUseCase(repository: auth)
        )
        let observer = Task { await viewModel.observeRecordCounts() }
        defer { observer.cancel() }

        #expect(await eventually { viewModel.recordCounts == ProfileRecordCounts(watched: 2, wishlist: 1) })
    }
}

@Suite struct StatsUiStateTests {
    private let november = YearMonth(year: 2026, month: 11)

    private func watched(_ id: Int, _ date: LocalDate?, rating: Double? = nil) -> MovieRecord {
        MovieRecord(id: "w\(id)", tmdbId: id, title: "W\(id)", status: .watched, rating: rating, watchedAt: date)
    }

    @Test func summarizesTotalsAndAverage() {
        let state = StatsUiState(records: [
            watched(1, LocalDate(year: 2026, month: 11, day: 2), rating: 4),
            watched(2, LocalDate(year: 2026, month: 10, day: 5), rating: 4.5),
            watched(3, nil, rating: 4.5),
            MovieRecord(id: "x", tmdbId: 9, title: "X", status: .wishlist),
        ], currentMonth: november)

        #expect(state.totalWatched == 3)
        #expect(state.wishlistCount == 1)
        #expect(state.averageRatingText == "4.3")
    }

    @Test func monthlyCountsEndWithTheCurrentMonth() {
        let state = StatsUiState(records: [
            watched(1, LocalDate(year: 2026, month: 11, day: 2)),
            watched(2, LocalDate(year: 2026, month: 6, day: 30)),
            watched(3, LocalDate(year: 2026, month: 5, day: 31)),
        ], currentMonth: november)

        #expect(state.monthlyCounts.map(\.month) == (6...11).map { YearMonth(year: 2026, month: $0) })
        #expect(state.monthlyCounts.map(\.count) == [1, 0, 0, 0, 0, 1])
        #expect(state.chartMaximum == 1)
    }

    @Test func comparesWithLastMonth() {
        func comparison(this: Int, last: Int) -> MonthComparison {
            var state = StatsUiState()
            state.thisMonthCount = this
            state.lastMonthCount = last
            return state.comparison
        }
        #expect(comparison(this: 0, last: 0) == MonthComparison.none)
        #expect(comparison(this: 6, last: 3) == .more(3))
        #expect(comparison(this: 1, last: 4) == .fewer(3))
        #expect(comparison(this: 2, last: 2) == .same)

        let state = StatsUiState(records: [
            watched(1, LocalDate(year: 2026, month: 11, day: 1)),
            watched(2, LocalDate(year: 2026, month: 10, day: 1)),
            watched(3, LocalDate(year: 2026, month: 10, day: 31)),
        ], currentMonth: november)
        #expect(state.thisMonthCount == 1)
        #expect(state.lastMonthCount == 2)
    }

    @Test func emptyStateHasNoAverageAndASafeMaximum() {
        let state = StatsUiState(records: [], currentMonth: november)
        #expect(state.averageRatingText == "-")
        #expect(state.chartMaximum == 1)
        #expect(state.monthlyCounts.count == 6)
    }
}

@MainActor
struct RecordPhotosViewModelTests {
    @Test func signsPathsInOrder() async {
        let photos = InMemoryPhotoRepository()
        let viewModel = RecordPhotosViewModel(getSignedPhotoUrls: GetSignedPhotoUrlsUseCase(repository: photos))

        await viewModel.load(paths: ["u/1/a.jpg", "u/1/b.jpg"])

        guard case .loaded(let urls) = viewModel.state else {
            Issue.record("expected loaded, got \(viewModel.state)")
            return
        }
        #expect(urls["u/1/a.jpg"]?.lastPathComponent == "a.jpg")
        #expect(urls["u/1/b.jpg"]?.lastPathComponent == "b.jpg")
        #expect(photos.signedPaths == [["u/1/a.jpg", "u/1/b.jpg"]])
    }

    @Test func noPathsMakesNoRequest() async {
        let photos = InMemoryPhotoRepository()
        let viewModel = RecordPhotosViewModel(getSignedPhotoUrls: GetSignedPhotoUrlsUseCase(repository: photos))

        await viewModel.load(paths: [])

        #expect(viewModel.state == .loaded([:]))
        #expect(photos.signedPaths.isEmpty)
    }

    @Test func reportsFailureAndRetries() async {
        let photos = InMemoryPhotoRepository()
        photos.signingError = URLError(.notConnectedToInternet)
        let viewModel = RecordPhotosViewModel(getSignedPhotoUrls: GetSignedPhotoUrlsUseCase(repository: photos))

        await viewModel.load(paths: ["u/1/a.jpg"])
        guard case .failed = viewModel.state else {
            Issue.record("expected failed, got \(viewModel.state)")
            return
        }

        photos.signingError = nil
        await viewModel.load(paths: ["u/1/a.jpg"])
        #expect(viewModel.state == .loaded(["u/1/a.jpg": URL(string: "https://example.invalid/sign/u/1/a.jpg")!]))
    }
}

/// Photo handling of UpsertRecordUseCase / DeleteRecordUseCase, with the in-memory repositories
@MainActor
struct RecordPhotoFlowTests {
    private let kept = "u1/10/kept.jpg"
    private let removed = "u1/10/removed.jpg"

    private func record(photos: [String]) -> MovieRecord {
        MovieRecord(id: "r1", userId: "u1", tmdbId: 10, title: "영화", status: .watched, photoUrls: photos)
    }

    @Test func savingUploadsNewPhotosAndDeletesRemovedOnes() async throws {
        let records = InMemoryRecordRepository(records: [record(photos: [kept, removed])])
        let photos = InMemoryPhotoRepository(stored: [kept, removed])
        let session = RecordEditSession(upsertRecord: UpsertRecordUseCase(repository: records, photoRepository: photos))

        session.select(record(photos: [kept, removed]))
        session.draft.keptPhotoPaths.removeAll { $0 == removed }
        session.draft.attach([Data([1]), Data([2])])
        await session.save()

        #expect(session.saveState == .success)
        let saved = try #require(records.allRecords.first)
        #expect(saved.photoUrls.count == 3)
        #expect(saved.photoUrls.first == kept)
        #expect(saved.photoUrls.dropFirst().allSatisfy { $0.hasPrefix("u1/10/") })
        #expect(photos.deletedPaths == [removed])
        #expect(photos.stored == Set(saved.photoUrls))
    }

    @Test func failedSaveRemovesTheJustUploadedPhotos() async {
        let records = InMemoryRecordRepository(records: [record(photos: [kept])])
        records.upsertError = URLError(.notConnectedToInternet)
        let photos = InMemoryPhotoRepository(stored: [kept])
        let upsert = UpsertRecordUseCase(repository: records, photoRepository: photos)

        await #expect(throws: URLError.self) {
            try await upsert(self.record(photos: [kept]), newPhotos: [Data([1])])
        }
        #expect(photos.stored == [kept])
        #expect(records.allRecords.first?.photoUrls == [kept])
    }

    @Test func failedPhotoDeleteKeepsTheSave() async throws {
        let records = InMemoryRecordRepository(records: [record(photos: [kept, removed])])
        let photos = InMemoryPhotoRepository(stored: [kept, removed])
        photos.deleteError = URLError(.timedOut)

        try await UpsertRecordUseCase(repository: records, photoRepository: photos)(record(photos: [kept]))

        #expect(records.allRecords.first?.photoUrls == [kept])
    }

    @Test func deletingARecordDeletesItsPhotos() async throws {
        let records = InMemoryRecordRepository(records: [record(photos: [kept, removed])])
        let photos = InMemoryPhotoRepository(stored: [kept, removed])

        try await DeleteRecordUseCase(repository: records, photoRepository: photos)(recordId: "r1")

        #expect(records.allRecords.isEmpty)
        #expect(photos.stored.isEmpty)
    }

    @Test func deletingStillWorksWhenPhotoCleanupFails() async throws {
        let records = InMemoryRecordRepository(records: [record(photos: [kept])])
        let photos = InMemoryPhotoRepository(stored: [kept])
        photos.deleteError = URLError(.timedOut)

        try await DeleteRecordUseCase(repository: records, photoRepository: photos)(recordId: "r1")

        #expect(records.allRecords.isEmpty)
    }

    @Test func draftKeepsPhotosAndStopsAtTheLimit() {
        var draft = RecordDraft(record: record(photos: [kept]))
        #expect(draft.keptPhotoPaths == [kept])
        draft.attach((0..<12).map { Data([UInt8($0)]) })
        #expect(draft.photoCount == RecordDraft.maxPhotos)
        #expect(draft.remainingPhotoSlots == 0)

        draft.keptPhotoPaths = []
        #expect(draft.applied(to: record(photos: [kept])).photoUrls.isEmpty)
    }

    @Test func draftSkipsPhotosThatAreAlreadyAttached() {
        var draft = RecordDraft(record: record(photos: [kept]))
        // "saved" is the library item behind the kept photo
        let skipped = draft.attach(
            [
                AttachedPhoto(data: Data([1]), sourceId: "saved"),
                AttachedPhoto(data: Data([2]), sourceId: "a"),
                AttachedPhoto(data: Data([2]), sourceId: "a")
            ],
            knownSourceIds: ["saved"]
        )
        #expect(skipped == 2)
        #expect(draft.newPhotos.map(\.sourceId) == ["a"])

        // Picking the same item again later in the session
        #expect(draft.attach([AttachedPhoto(data: Data([2]), sourceId: "a")]) == 1)
        // Without an identifier the data decides
        draft.attach([Data([2]), Data([3])])
        #expect(draft.newPhotos.count == 2)
    }

    @Test func savingRemembersWhereAlbumPhotosCameFrom() async throws {
        let defaults = try #require(UserDefaults(suiteName: "PhotoSourceIndexTests-\(UUID().uuidString)"))
        let index = PhotoSourceIndex(defaults: defaults)
        let records = InMemoryRecordRepository(records: [record(photos: [])])
        let session = RecordEditSession(
            upsertRecord: UpsertRecordUseCase(repository: records, photoRepository: InMemoryPhotoRepository(stored: [])),
            photoSources: index
        )

        session.select(record(photos: []))
        session.draft.attach([AttachedPhoto(data: Data([1]), sourceId: "album-1"), AttachedPhoto(data: Data([2]))])
        await session.save()

        let saved = try #require(records.allRecords.first)
        #expect(index.sourceIds(for: saved.photoUrls) == ["album-1"])
        // Survives a restart
        #expect(PhotoSourceIndex(defaults: defaults).sourceIds(for: saved.photoUrls) == ["album-1"])
    }
}

@MainActor
private final class FakeCameraDevice: CameraDevice {
    var granted = true
    var startError: Error?
    var photo = Data([0xFF, 0xD8])
    private(set) var isRunning = false
    private(set) var captures = 0

    func requestAccess() async -> Bool { granted }
    func start() async throws {
        if let startError { throw startError }
        isRunning = true
    }
    func stop() { isRunning = false }
    func capturePhoto() async throws -> Data {
        captures += 1
        return photo
    }
}

@MainActor
private final class FakeLibrary: PhotoLibrarySaving {
    private(set) var saved: [Data] = []
    func save(_ jpeg: Data) async { saved.append(jpeg) }
}

@MainActor
struct CameraViewModelTests {
    @Test func capturesReviewsAndUsesAPhoto() async {
        let device = FakeCameraDevice()
        let library = FakeLibrary()
        let viewModel = CameraViewModel(device: device, library: library)

        await viewModel.start()
        #expect(viewModel.phase == .preview)
        #expect(device.isRunning)

        await viewModel.capture()
        #expect(viewModel.phase == .review(device.photo))

        viewModel.retake()
        #expect(viewModel.phase == .preview)

        await viewModel.capture()
        #expect(viewModel.usePhoto() == device.photo)
        #expect(device.captures == 2)
        #expect(await eventually { library.saved == [device.photo] })

        viewModel.stop()
        #expect(!device.isRunning)
    }

    @Test func showsDeniedWithoutAccess() async {
        let device = FakeCameraDevice()
        device.granted = false
        let viewModel = CameraViewModel(device: device, library: nil)

        await viewModel.start()
        #expect(viewModel.phase == .denied)
        await viewModel.capture()
        #expect(device.captures == 0)
        #expect(viewModel.usePhoto() == nil)
    }

    @Test func reportsAStartFailure() async {
        let device = FakeCameraDevice()
        device.startError = URLError(.unknown)
        let viewModel = CameraViewModel(device: device, library: nil)

        await viewModel.start()
        guard case .error = viewModel.phase else {
            Issue.record("expected error, got \(viewModel.phase)")
            return
        }
    }
}
