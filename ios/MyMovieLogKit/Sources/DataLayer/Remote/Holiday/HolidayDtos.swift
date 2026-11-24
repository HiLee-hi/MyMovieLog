import Foundation

// getRestDeInfo items by count: "" for 0, {"item": {...}} for 1, {"item": [...]} for 2+
struct HolidayResponseDto: Decodable {
    let response: Wrapper

    struct Wrapper: Decodable {
        let header: HolidayHeaderDto
        let body: HolidayBodyDto?
    }
}

struct HolidayHeaderDto: Decodable {
    let resultCode: String
    let resultMsg: String
}

struct HolidayBodyDto: Decodable {
    let items: [HolidayItemDto]

    private enum CodingKeys: String, CodingKey { case items }
    private struct ItemsContainer: Decodable {
        let item: OneOrMany
    }
    private enum OneOrMany: Decodable {
        case one(HolidayItemDto)
        case many([HolidayItemDto])

        init(from decoder: Decoder) throws {
            let container = try decoder.singleValueContainer()
            if let list = try? container.decode([HolidayItemDto].self) {
                self = .many(list)
            } else {
                self = .one(try container.decode(HolidayItemDto.self))
            }
        }
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        // "" (no holidays) fails as an object -> empty list
        switch try? container.decodeIfPresent(ItemsContainer.self, forKey: .items)?.item {
        case .one(let item): items = [item]
        case .many(let list): items = list
        case nil: items = []
        }
    }
}

struct HolidayItemDto: Decodable {
    let dateName: String
    // yyyyMMdd as a number, e.g. 20260924
    let locdate: Int
    let isHoliday: String
}
