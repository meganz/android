package mega.privacy.android.domain.entity.chat

/**
 * Chat room changes.
 */
enum class ChatRoomChanges {
    /**
     * Chat room status has changed
     */
    Status,

    /**
     * The number of unread messages has changed
     */
    UnreadCount,

    /**
     * Participants have changed
     */
    Participants,

    /**
     * Title of chatroom has changed
     */
    Title,

    /**
     * User is typing
     */
    UserTyping,

    /**
     * The chatroom has been left by own user
     */
    Closed,

    /**
     * Our privilege level has changed
     */
    OwnPrivilege,

    /**
     * User has stopped to typing
     */
    UserStopTyping,

    /**
     * Archived or unarchived
     */
    Archive,

    /**
     * User has set chat mode to private
     */
    ChatMode,

    /**
     * The number of previewers has changed
     */
    UpdatePreviewers,

    /**
     * The retention time has changed
     */
    RetentionTime,

    /**
     * The open invite mode option has changed
     */
    OpenInvite,

    /**
     * The speak request option has changed
     */
    SpeakRequest,

    /**
     * The waiting room option has changed
     */
    WaitingRoom,
}