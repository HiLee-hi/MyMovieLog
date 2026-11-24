@testable import DataLayer
import Domain
import Foundation
import Testing

struct HolidayDtoTests {
    private func decode(_ json: String) throws -> HolidayResponseDto {
        try JSONDecoder().decode(HolidayResponseDto.self, from: Data(json.utf8))
    }

    @Test func decodesSeveralItems() throws {
        let response = try decode("""
        {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},"body":{"items":{"item":[
          {"dateKind":"01","dateName":"추석","isHoliday":"Y","locdate":20260924,"seq":1},
          {"dateKind":"01","dateName":"추석","isHoliday":"Y","locdate":20260925,"seq":1}
        ]},"numOfRows":100,"pageNo":1,"totalCount":2}}}
        """)
        #expect(response.response.body?.items.map(\.locdate) == [20260924, 20260925])
    }

    @Test func decodesASingleItemObject() throws {
        let response = try decode("""
        {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},"body":{"items":{"item":
          {"dateKind":"01","dateName":"광복절","isHoliday":"Y","locdate":20260815,"seq":1}
        },"numOfRows":100,"pageNo":1,"totalCount":1}}}
        """)
        #expect(response.response.body?.items.map(\.dateName) == ["광복절"])
    }

    @Test func decodesAMonthWithoutHolidays() throws {
        let response = try decode("""
        {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},"body":{"items":"","numOfRows":100,"pageNo":1,"totalCount":0}}}
        """)
        #expect(response.response.body?.items.isEmpty == true)
    }

    @Test func encodesTheKeyAndPadsTheMonth() {
        let url = HolidayApi.url(serviceKey: "a+b/c==", year: 2026, month: 9).absoluteString
        #expect(url.contains("serviceKey=a%2Bb%2Fc%3D%3D"))
        #expect(url.contains("solYear=2026"))
        #expect(url.contains("solMonth=09"))
        #expect(url.contains("_type=json"))
    }

    @Test func parsesBasicIsoDates() {
        #expect(LocalDate(basicIsoNumber: 20260924) == LocalDate(year: 2026, month: 9, day: 24))
        #expect(LocalDate(basicIsoNumber: 20261301) == nil)
    }
}

/// Answers every request with the next queued response and counts the requests
final class HolidayStubProtocol: URLProtocol, @unchecked Sendable {
    nonisolated(unsafe) static var responses: [(status: Int, body: String)] = []
    nonisolated(unsafe) static var requestCount = 0

    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }

    override func startLoading() {
        Self.requestCount += 1
        let next = Self.responses.isEmpty ? (status: 500, body: "") : Self.responses.removeFirst()
        let response = HTTPURLResponse(url: request.url!, statusCode: next.status, httpVersion: nil, headerFields: nil)!
        client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        client?.urlProtocol(self, didLoad: Data(next.body.utf8))
        client?.urlProtocolDidFinishLoading(self)
    }

    override func stopLoading() {}
}

/// Uses the shared stub, so the cases run one after another
@Suite(.serialized) struct HolidayRepositoryTests {
    private let september = YearMonth(year: 2026, month: 9)
    private let chuseok = """
    {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},"body":{"items":{"item":[
      {"dateName":"추석","isHoliday":"Y","locdate":20260924},
      {"dateName":"추석","isHoliday":"Y","locdate":20260925},
      {"dateName":"기념일","isHoliday":"N","locdate":20260930}
    ]},"totalCount":3}}}
    """

    private func makeRepository(responses: [(status: Int, body: String)]) -> HolidayRepositoryImpl {
        HolidayStubProtocol.responses = responses
        HolidayStubProtocol.requestCount = 0
        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [HolidayStubProtocol.self]
        return HolidayRepositoryImpl(
            api: HolidayApi(serviceKey: "key", session: URLSession(configuration: configuration)),
            cache: HolidayCache(fileURL: nil)
        )
    }

    @Test func fetchesOnceThenUsesTheCache() async {
        let repository = makeRepository(responses: [(200, chuseok)])

        let first = await repository.holidays(in: september)
        let second = await repository.holidays(in: september)

        #expect(first == [LocalDate(year: 2026, month: 9, day: 24), LocalDate(year: 2026, month: 9, day: 25)])
        #expect(second == first)
        #expect(HolidayStubProtocol.requestCount == 1)
    }

    @Test func doesNotCacheFailures() async {
        let serviceError = #"{"response":{"header":{"resultCode":"30","resultMsg":"SERVICE KEY IS NOT REGISTERED ERROR."}}}"#
        let repository = makeRepository(responses: [(200, serviceError), (500, ""), (200, chuseok)])

        #expect(await repository.holidays(in: september).isEmpty)
        #expect(await repository.holidays(in: september).isEmpty)
        #expect(await repository.holidays(in: september).count == 2)
        #expect(HolidayStubProtocol.requestCount == 3)
    }

    @Test func showsNothingWithoutAKey() async {
        let repository = HolidayRepositoryImpl(api: nil, cache: HolidayCache(fileURL: nil))
        #expect(await repository.holidays(in: september).isEmpty)
    }

    @Test func cacheSurvivesAReload() async throws {
        let file = FileManager.default.temporaryDirectory.appending(path: "holidays-\(UUID().uuidString).json")
        defer { try? FileManager.default.removeItem(at: file) }
        let empty = YearMonth(year: 2026, month: 11)

        await HolidayCache(fileURL: file).store([LocalDate(year: 2026, month: 9, day: 24)], in: september)
        await HolidayCache(fileURL: file).store([], in: empty)

        let reloaded = HolidayCache(fileURL: file)
        #expect(await reloaded.holidays(in: september) == [LocalDate(year: 2026, month: 9, day: 24)])
        // A month without holidays is remembered, so it is not requested again
        #expect(await reloaded.holidays(in: empty) == [])
        #expect(await reloaded.holidays(in: YearMonth(year: 2026, month: 10)) == nil)
    }
}
