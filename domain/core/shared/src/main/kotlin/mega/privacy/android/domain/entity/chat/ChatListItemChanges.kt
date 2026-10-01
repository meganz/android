package mega.privacy.android.domain.entity.chat

/**
 * Chat room list item changes.
 */
enum class ChatListItemChanges {
    /**
     * Chat room status has changed
     */
    Status,

    /**
     * Own privileges has changed
     */
    OwnPrivilege,

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
     * The chatroom has been left by own user
     */
    Closed,

    /**
     * Last message has changed
     */
    LastMessage,

    /**
     * Last timestamp has changed
     */
    LastTS,

    /**
     * Archived or unarchived
     */
    Archive,

    /**
     * Call in that chat
     */
    Call,

    /**
     * User has set chat mode to private
     */
    ChatMode,

    /**
     * The number of previewers has changed
     */
    UpdatePreviewers,

    /**
     * Preview closed
     */
    PreviewClosed,

    /**
     * Deleted
     */
    Deleted,

    /**
     * Unknown
     */
    Unknown,
}