/// Reads come from the local cache (offline support); writes go to Supabase first.
public protocol MovieRecordRepository: Sendable {
    func getRecords(status: WatchStatus?) -> AsyncStream<[MovieRecord]>
    func getRecordByTmdbId(_ tmdbId: Int) -> AsyncStream<MovieRecord?>
    func getRecordsByDate(_ date: LocalDate) -> AsyncStream<[MovieRecord]>
    func getWatchedDatesByMonth(_ month: YearMonth) -> AsyncStream<Set<LocalDate>>
    func upsertRecord(_ record: MovieRecord) async throws
    func deleteRecord(id: String) async throws
    func syncFromRemote() async throws
    func clearLocalCache() async throws
}
