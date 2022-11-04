package mega.privacy.android.app.presentation.meeting.model

/**
 * Enum class defining the available actions for scheduled meeting info screen.
 */
enum class ScheduledMeetingInfoAction {

    /**
     * Create or remove meeting link.
     */
    MeetingLink,

    /**
     * Share meeting link.
     */
    ShareMeetingLink,

    /**
     * Enable or disable chat notifications.
     */
    ChatNotifications,

    /**
     * Allow non-hosts add participants to the chat.
     */
    AllowNonHostAddParticipants,

    /**
     * Share files.
     */
    ShareFiles,

    /**
     * Manage Chat history.
     */
    ManageChatHistory,
}