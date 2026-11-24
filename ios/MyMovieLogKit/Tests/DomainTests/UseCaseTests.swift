import Domain
import Testing

private struct SignedOutAuthRepository: AuthRepository {
    func observeCurrentUser() -> AsyncStream<UserProfile?> { AsyncStream { $0.yield(nil) } }
    func currentUserId() async -> String? { nil }
    func signInWithEmail(email: String, password: String) async throws {}
    func signUpWithEmail(email: String, password: String) async throws {}
    func signInWithGoogle() async throws {}
    func signOut() async throws {}
}

struct GetCurrentUserIdUseCaseTests {
    @Test func throwsWhenSignedOut() async {
        let useCase = GetCurrentUserIdUseCase(repository: SignedOutAuthRepository())
        await #expect(throws: DomainError.notSignedIn) {
            try await useCase()
        }
    }
}

struct MovieTests {
    @Test func buildsImageUrlsAndYear() {
        let movie = Movie(id: 1, title: "t", originalTitle: "t", overview: "", posterPath: "/p.jpg",
                          backdropPath: nil, releaseDate: "2019-05-30", voteAverage: 0, genreIds: [])
        #expect(movie.posterURL?.absoluteString == "https://image.tmdb.org/t/p/w500/p.jpg")
        #expect(movie.backdropURL == nil)
        #expect(movie.releaseYear == "2019")
    }

    @Test func hasNoYearWithoutReleaseDate() {
        let movie = Movie(id: 1, title: "t", originalTitle: "t", overview: "", posterPath: "",
                          backdropPath: nil, releaseDate: "", voteAverage: 0, genreIds: [])
        #expect(movie.releaseYear == nil)
        #expect(movie.posterURL == nil)
    }
}
