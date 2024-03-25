package mega.privacy.android.app.presentation.search.model

/**
 * Enum class to represent the date modified filter options
 *
 * @param title The title of the filter option
 */
enum class DateFilterOption(val title: String) {
    /**
     * today filter option
     */
    Today("Today"),

    /**
     * last 7 days filter option
     */
    Last7Days("Last 7 days"),

    /**
     * last 30 days filter option
     */
    Last30Days("Last 30 days"),

    /**
     * this year filter option
     */
    ThisYear("This year"),

    /**
     * last year filter option
     */
    LastYear("Last year"),

    /**
     * older filter option
     */
    Older("Older"),
}
