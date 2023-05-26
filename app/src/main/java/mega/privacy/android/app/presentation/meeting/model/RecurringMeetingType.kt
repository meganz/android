package mega.privacy.android.app.presentation.meeting.model

/**
 * Types of recurring meeting
 */
enum class RecurringMeetingType {
    /**
     * Invalid value
     */
    Never,

    /**
     * Occurs daily
     */
    Daily,

    /**
     * Occurs weekly
     */
    Weekly,

    /**
     * Occurs monthly
     */
    Monthly,

    /**
     * Custom
     */
    Custom
}