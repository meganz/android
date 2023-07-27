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
     * Start Pending Meeting tooltip
     */
    PENDING,

    /**
     * Manage Recurring Meeting tooltip
     */
    RECURRING,

    /**
     * All tooltips have been shown
     */
    NONE
}
