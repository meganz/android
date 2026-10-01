package mega.privacy.android.domain.entity.chat

/**
 * Current Chat status
 */
enum class ChatStatus {
    /**
     * No network connection
     */
    NoNetworkConnection,

    /**
     * Reconnecting
     */
    Reconnecting,

    /**
     * Connecting
     */
    Connecting,

    /**
     * Online
     */
    Online,

    /**
     * Away
     */
    Away,

    /**
     * Busy
     */
    Busy,

    /**
     * Offline
     */
    Offline
}
