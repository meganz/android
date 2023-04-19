package mega.privacy.android.domain.entity.chat

/**
 * Chat connection state
 */
enum class ChatConnectionStatus {

    /**
     * Chat connection state is offline
     */
    Offline,

    /**
     * Chat connection state is in progress
     */
    InProgress,

    /**
     * Chat connection state is Logging
     */
    Logging,

    /**
     * Chat connection state is online
     */
    Online,

    /**
     * Chat connection state is unknown
     */
    Unknown,
}
