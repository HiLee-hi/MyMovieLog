import Foundation

enum HolidayApiError: Error {
    case http(statusCode: Int)
    // resultCode != "00", e.g. unregistered key
    case service(code: String, message: String)
}

// 한국천문연구원 특일 정보 (공공데이터포털), same endpoint as android but HTTPS
struct HolidayApi: Sendable {
    private static let endpoint = "https://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo"

    // decoded key (마이페이지 -> 인증키 -> 디코딩)
    private let serviceKey: String
    private let session: URLSession

    init(serviceKey: String, session: URLSession = .shared) {
        self.serviceKey = serviceKey
        self.session = session
    }

    // days off (`isHoliday == "Y"`) in the month
    func holidayItems(year: Int, month: Int) async throws -> [HolidayItemDto] {
        let (data, response) = try await session.data(from: Self.url(serviceKey: serviceKey, year: year, month: month))
        if let http = response as? HTTPURLResponse, !(200..<300).contains(http.statusCode) {
            throw HolidayApiError.http(statusCode: http.statusCode)
        }
        let decoded = try JSONDecoder().decode(HolidayResponseDto.self, from: data)
        let header = decoded.response.header
        guard header.resultCode == "00" else {
            throw HolidayApiError.service(code: header.resultCode, message: header.resultMsg)
        }
        return (decoded.response.body?.items ?? []).filter { $0.isHoliday == "Y" }
    }

    // URLQueryItem keeps "+" (server reads a space) -> encode by hand, all but unreserved chars
    static func url(serviceKey: String, year: Int, month: Int) -> URL {
        let unreserved = CharacterSet(charactersIn: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~")
        let key = serviceKey.addingPercentEncoding(withAllowedCharacters: unreserved) ?? serviceKey
        let query = [
            "serviceKey=\(key)",
            "solYear=\(year)",
            "solMonth=\(String(format: "%02d", month))",
            "_type=json",
            "numOfRows=100",
        ].joined(separator: "&")
        return URL(string: "\(endpoint)?\(query)")!
    }
}
