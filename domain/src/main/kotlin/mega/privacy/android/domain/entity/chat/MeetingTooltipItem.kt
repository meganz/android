package mega.privacy.android.domain.entity.chat

/**
 * Enum class representing Scheduled Meetings tooltips to be shown
 */
enum class MeetingTooltipItem {
    /**
     * Create Schedule Meeting Fab tooltip
     */
    CREATE,

    /**
     * Manage Recurring or Start Pending Meeting tooltips
     */
    RECURRING_OR_PENDING,

    /**
     * Manage Recurring Meeting tooltip
     */
    RECURRING,

    /**
     * Start Pending Meeting tooltip
     */
    PENDING,

    /**
     * All tooltips have been shown
     */
    NONE
}
