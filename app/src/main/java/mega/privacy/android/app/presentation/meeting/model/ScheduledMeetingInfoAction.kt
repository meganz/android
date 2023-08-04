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
     * Enable or disable waiting room.
     */
    WaitingRoom,

    /**
     * Allow non-hosts add participants to the chat.
     */
    AllowNonHostAddParticipants,

    /**
     * Share files.
     */
    ShareFiles,

    /**
     * Share meeting link option for non-hosts.
     */
    ShareMeetingLinkNonHosts,

    /**
     * Manage Chat history.
     */
    ManageChatHistory,

    /**
     * Enable Encrypted key rotation.
     */
    EnableEncryptedKeyRotation,

    /**
     * Enabled Encrypted key rotation.
     */
    EnabledEncryptedKeyRotation,
}