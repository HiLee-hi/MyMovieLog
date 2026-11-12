import OSLog

enum Log {
    static let app = Logger(subsystem: "com.mymovie.log", category: "APP_INIT")
    static let auth = Logger(subsystem: "com.mymovie.log", category: "REPO_AUTH")
    static let record = Logger(subsystem: "com.mymovie.log", category: "REPO_RECORD")
    static let movie = Logger(subsystem: "com.mymovie.log", category: "REPO_MOVIE")
}
