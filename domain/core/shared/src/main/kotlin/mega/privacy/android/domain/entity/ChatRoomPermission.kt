package mega.privacy.android.domain.entity

/**
 * Chat room permission.
 */
enum class ChatRoomPermission {
    /**
     * Unknown privilege
     */
    Unknown,

    /**
     * Removed privilege
     */
    Removed,

    /**
     * Read only privilege
     */
    ReadOnly,

    /**
     * Standard privilege
     */
    Standard,

    /**
     * Moderator privilege
     */
    Moderator
}