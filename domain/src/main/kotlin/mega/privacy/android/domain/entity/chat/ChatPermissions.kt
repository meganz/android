package mega.privacy.android.domain.entity.chat

/**
 * Enum class defining the possible participant permissions in the chat.
 */
enum class ChatPermissions {

    /**
     * Host permission.
     */
    Host,

    /**
     * Standard permission.
     */
    Standard,

    /**
     * Read only permission.
     */
    ReadOnly,

    /**
     * Removed permission.
     */
    Removed,
}