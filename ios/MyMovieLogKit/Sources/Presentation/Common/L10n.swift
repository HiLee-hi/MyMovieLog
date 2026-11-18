import Foundation

/// Typed access to `Localizable.xcstrings`, the counterpart of Android's `R.string`.
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
    static func libraryWatchedTab(_ arg0: Int) -> String { format("library.watched_tab", arg0) }
    static func libraryWishlistTab(_ arg0: Int) -> String { format("library.wishlist_tab", arg0) }
    static func recordDeleteConfirm(_ arg0: String) -> String { format("record.delete_confirm", arg0) }
    static func recordPhotosKept(_ arg0: Int) -> String { format("record.photos_kept", arg0) }
    static func recordStar(_ arg0: Int) -> String { format("record.star", arg0) }
    static func recordRatingValue(_ arg0: String) -> String { format("record.rating_value", arg0) }
    static func recordRatingLabel(_ arg0: String) -> String { format("record.rating_label", arg0) }
    static func recordRatingSummary(_ arg0: String) -> String { format("record.rating_summary", arg0) }
    static func detailWatchedOn(_ arg0: String) -> String { format("detail.watched_on", arg0) }
    static func detailPhotoCount(_ arg0: Int) -> String { format("detail.photo_count", arg0) }

    /// Sunday first
    static var weekdays: [String] {
        ["weekday.sun", "weekday.mon", "weekday.tue", "weekday.wed", "weekday.thu", "weekday.fri", "weekday.sat"].map(text)
    }

    /// `DateFormatter` patterns, such as "yyyy년 M월 d일" or "MMM d, yyyy"
    static var datePattern: String { text("format.date") }
    static var yearMonthPattern: String { text("format.year_month") }

    /// The language the strings resolved to, so dates and text never mix languages.
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

/// Text for the app target, which shows it before the main UI can start.
public enum ConfigurationStrings {
    public static var missingKeyTitle: String { L10n.text("config.missing_key_title") }
    public static var missingKeyMessage: String { L10n.text("config.missing_key_message") }
}
