package mega.privacy.android.app.presentation.meeting.chat.model.messages.actions

/**
 * Enum class for defining all the possible action groups for messages
 */
enum class MessageActionGroup {

    /**
     * Open actions:
     * - Open with.
     */
    Open,

    /**
     * Contact actions:
     * - Info.
     * - Send message.
     * - Invite.
     */
    Contact,

    /**
     * Modify actions:
     * - Edit.
     * - Copy.
     */
    Modify,

    /**
     * Share actions:
     * - Forward.
     * - Share.
     */
    Share,

    /**
     * Select actions:
     * - Select.
     */
    Select,

    /**
     * Transfer actions:
     * - Add to Cloud drive.
     * - Save to device.
     * - Available offline.
     */
    Transfer,

    /**
     * Retry actions:
     * - Retry.
     */
    Retry,

    /**
     * Delete actions:
     * - Delete.
     */
    Delete,
}