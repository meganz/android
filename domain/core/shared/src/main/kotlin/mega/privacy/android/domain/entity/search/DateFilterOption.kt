package mega.privacy.android.domain.entity.search

/**
 * Enum class to represent the date modified filter options
 */
enum class DateFilterOption {
    /**
     * today filter option
     */
    Today,

    /**
     * last 7 days filter option
     */
    Last7Days,

    /**
     * last 30 days filter option
     */
    Last30Days,

    /**
     * this year filter option
     */
    ThisYear,

    /**
     * last year filter option
     */
    LastYear,

    /**
     * older filter option
     */
    Older,
}
