package mega.privacy.android.domain.entity.meeting

/**
 * Types of recurring meeting
 */
enum class RecurringMeetingType {
    /**
     * Occurs never
     */
    Never,

    /**
     * Occurs every day
     */
    EveryDay,

    /**
     * Occurs every week
     */
    EveryWeek,

    /**
     * Occurs every month
     */
    EveryMonth,

    /**
     * Custom
     */
    Custom,

    /**
     * Customised
     */
    Customised,

}