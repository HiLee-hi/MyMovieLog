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
    @Test func followsMonthAndSelectedDate() async {
        let today = LocalDate.today()
        let records = InMemoryRecordRepository()
        let viewModel = CalendarViewModel(
            getWatchedDatesByMonth: GetWatchedDatesByMonthUseCase(repository: records),
            getRecordsByDate: GetRecordsByDateUseCase(repository: records),
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
}
