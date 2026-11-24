import Foundation

// typed Localizable.xcstrings access, like android R.string
enum L10n {
    static var appName: String { text("app.name") }
    static var commonSave: String { text("common.save") }
    static var commonCancel: String { text("common.cancel") }
    static var commonDelete: String { text("common.delete") }
    static var commonClose: String { text("common.close") }
    static var commonConfirm: String { text("common.confirm") }
    static var commonRetry: String { text("common.retry") }
    static var commonSignIn: String { text("common.sign_in") }
    static var commonThisMonth: String { text("common.this_month") }
    static var commonTotalWatched: String { text("common.total_watched") }
    static var tabHome: String { text("tab.home") }
    static var tabSearch: String { text("tab.search") }
    static var tabLibrary: String { text("tab.library") }
    static var tabStats: String { text("tab.stats") }
    static var tabProfile: String { text("tab.profile") }
    static var homeGreeting: String { text("home.greeting") }
    static var homeLoginPrompt: String { text("home.login_prompt") }
    static var homeRecent: String { text("home.recent") }
    static var homeRecentEmpty: String { text("home.recent_empty") }
    static var homeWishlist: String { text("home.wishlist") }
    static var homeWishlistEmpty: String { text("home.wishlist_empty") }
    static var homeSummary: String { text("home.summary") }
    static var homeOpenCalendar: String { text("home.open_calendar") }
    static var homeShowAll: String { text("home.show_all") }
    static var calendarTitle: String { text("calendar.title") }
    static var calendarLegendWatched: String { text("calendar.legend_watched") }
    static var calendarPreviousMonth: String { text("calendar.previous_month") }
    static var calendarNextMonth: String { text("calendar.next_month") }
    static var calendarToday: String { text("calendar.today") }
    static var calendarNoRecords: String { text("calendar.no_records") }
    static var calendarEditHint: String { text("calendar.edit_hint") }
    static var calendarSelectDateTitle: String { text("calendar.select_date_title") }
    static var calendarSelectDateMessage: String { text("calendar.select_date_message") }
    static var libraryList: String { text("library.list") }
    static var libraryWatchedEmpty: String { text("library.watched_empty") }
    static var libraryWishlistEmpty: String { text("library.wishlist_empty") }
    static var libraryEmptyHint: String { text("library.empty_hint") }
    static var libraryDeleteFailed: String { text("library.delete_failed") }
    static var searchPrompt: String { text("search.prompt") }
    static var searchClear: String { text("search.clear") }
    static var searchSelectTitle: String { text("search.select_title") }
    static var searchSelectMessage: String { text("search.select_message") }
    static var searchStart: String { text("search.start") }
    static var searchNoResults: String { text("search.no_results") }
    static var searchFailed: String { text("search.failed") }
    static var recordStatus: String { text("record.status") }
    static var recordStatusWatched: String { text("record.status_watched") }
    static var recordStatusWishlist: String { text("record.status_wishlist") }
    static var recordRating: String { text("record.rating") }
    static var recordRatingNone: String { text("record.rating_none") }
    static var recordWatchedDate: String { text("record.watched_date") }
    static var recordDate: String { text("record.date") }
    static var recordReviewOptional: String { text("record.review_optional") }
    static var recordReviewPlaceholder: String { text("record.review_placeholder") }
    static var recordMemoOptional: String { text("record.memo_optional") }
    static var recordMemoPlaceholder: String { text("record.memo_placeholder") }
    static var recordDelete: String { text("record.delete") }
    static var recordPhotos: String { text("record.photos") }
    static var recordPhotosLoadFailed: String { text("record.photos_load_failed") }
    static var recordPhotoAlbum: String { text("record.photo_album") }
    static var recordPhotoCamera: String { text("record.photo_camera") }
    static var cameraHint: String { text("camera.hint") }
    static var cameraShutter: String { text("camera.shutter") }
    static var cameraRetake: String { text("camera.retake") }
    static var cameraUsePhoto: String { text("camera.use_photo") }
    static var cameraCapturedPhoto: String { text("camera.captured_photo") }
    static var cameraDenied: String { text("camera.denied") }
    static var cameraOpenSettings: String { text("camera.open_settings") }
    static var cameraUnavailable: String { text("camera.unavailable") }
    static var cameraCaptureFailed: String { text("camera.capture_failed") }
    static var recordAdd: String { text("record.add") }
    static var recordEdit: String { text("record.edit") }
    static var detailLoadFailed: String { text("detail.load_failed") }
    static var detailMyRecord: String { text("detail.my_record") }
    static var detailNoRecord: String { text("detail.no_record") }
    static var detailOverview: String { text("detail.overview") }
    static var loginRequiredTitle: String { text("login_required.title") }
    static var loginRequiredMessage: String { text("login_required.message") }
    static var loginRequiredAction: String { text("login_required.action") }
    static var profileDefaultName: String { text("profile.default_name") }
    static var profileSignOut: String { text("profile.sign_out") }
    static var statsLoginRequired: String { text("stats.login_required") }
    static var statsAverageRating: String { text("stats.average_rating") }
    static var statsWishlist: String { text("stats.wishlist") }
    static var statsWatchedMovies: String { text("stats.watched_movies") }
    static var statsVsLastMonth: String { text("stats.vs_last_month") }
    static var statsSameAsLastMonth: String { text("stats.same_as_last_month") }
    static var statsRecentSixMonths: String { text("stats.recent_six_months") }
    static var profileWatchedCount: String { text("profile.watched_count") }
    static var profileWishlistCount: String { text("profile.wishlist_count") }
    static var profileVerifyEmailTitle: String { text("profile.verify_email_title") }
    static var profileVerifyEmailMessage: String { text("profile.verify_email_message") }
    static var profileVerifiedSignIn: String { text("profile.verified_sign_in") }
    static var profileSignInPrompt: String { text("profile.sign_in_prompt") }
    static var profileSharedAccountHint: String { text("profile.shared_account_hint") }
    static var profileSignUp: String { text("profile.sign_up") }
    static var profileSignInEmail: String { text("profile.sign_in_email") }
    static var profileEmail: String { text("profile.email") }
    static var profilePassword: String { text("profile.password") }
    static var profileHaveAccount: String { text("profile.have_account") }
    static var profileNoAccount: String { text("profile.no_account") }
    static var profileSignInGoogle: String { text("profile.sign_in_google") }

    static func commonMovieCount(_ arg0: Int) -> String { format("common.movie_count", arg0) }
    static func calendarDay(_ arg0: Int) -> String { format("calendar.day", arg0) }
    static func calendarDayWatched(_ arg0: Int) -> String { format("calendar.day_watched", arg0) }
    static func statsVsLastMonthMore(_ arg0: Int) -> String { format("stats.vs_last_month_more", arg0) }
    static func statsVsLastMonthFewer(_ arg0: Int) -> String { format("stats.vs_last_month_fewer", arg0) }
    static func statsMonthBar(_ arg0: String, _ arg1: Int) -> String { format("stats.month_bar", arg0, arg1) }
    static func libraryWatchedTab(_ arg0: Int) -> String { format("library.watched_tab", arg0) }
    static func libraryWishlistTab(_ arg0: Int) -> String { format("library.wishlist_tab", arg0) }
    static func recordDeleteConfirm(_ arg0: String) -> String { format("record.delete_confirm", arg0) }
    static func recordPhotoNumber(_ arg0: Int) -> String { format("record.photo_number", arg0) }
    static func recordPhotoPosition(_ arg0: Int, _ arg1: Int) -> String { format("record.photo_position", arg0, arg1) }
    static func recordPhotoCount(_ arg0: Int, _ arg1: Int) -> String { format("record.photo_count", arg0, arg1) }
    static func recordPhotoDuplicatesSkipped(_ arg0: Int) -> String { format("record.photo_duplicates_skipped", arg0) }
    static func recordPhotoRemove(_ arg0: Int) -> String { format("record.photo_remove", arg0) }
    static func recordStar(_ arg0: Int) -> String { format("record.star", arg0) }
    static func recordRatingValue(_ arg0: String) -> String { format("record.rating_value", arg0) }
    static func recordRatingLabel(_ arg0: String) -> String { format("record.rating_label", arg0) }
    static func recordRatingSummary(_ arg0: String) -> String { format("record.rating_summary", arg0) }
    static func detailWatchedOn(_ arg0: String) -> String { format("detail.watched_on", arg0) }
    static func detailPhotoCount(_ arg0: Int) -> String { format("detail.photo_count", arg0) }

    // sunday first
    static var weekdays: [String] {
        ["weekday.sun", "weekday.mon", "weekday.tue", "weekday.wed", "weekday.thu", "weekday.fri", "weekday.sat"].map(text)
    }

    // DateFormatter patterns, e.g. "yyyy년 M월 d일", "MMM d, yyyy"
    static var datePattern: String { text("format.date") }
    static var yearMonthPattern: String { text("format.year_month") }
    static var monthPattern: String { text("format.month") }

    // resolved strings language, keeps dates and text in one language
    static var locale: Locale {
        Locale(identifier: Bundle.module.preferredLocalizations.first ?? "ko")
    }

    fileprivate static func text(_ key: String) -> String {
        Bundle.module.localizedString(forKey: key, value: nil, table: nil)
    }

    private static func format(_ key: String, _ arguments: CVarArg...) -> String {
        String(format: text(key), arguments: arguments)
    }
}

// app target text, shown before the main UI starts
public enum ConfigurationStrings {
    public static var missingKeyTitle: String { L10n.text("config.missing_key_title") }
    public static var missingKeyMessage: String { L10n.text("config.missing_key_message") }
}
