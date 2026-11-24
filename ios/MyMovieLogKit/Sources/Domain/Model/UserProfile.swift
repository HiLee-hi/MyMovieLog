public struct UserProfile: Equatable, Sendable {
    public let id: String
    public let email: String
    public let displayName: String?
    public let avatarUrl: String?

    public init(id: String, email: String, displayName: String?, avatarUrl: String?) {
        self.id = id
        self.email = email
        self.displayName = displayName
        self.avatarUrl = avatarUrl
    }
}
