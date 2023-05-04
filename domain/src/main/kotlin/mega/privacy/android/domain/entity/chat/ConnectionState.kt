package mega.privacy.android.domain.entity.chat

/**
 * Connection State
 *
 * current state of the connection
 * @property Disconnected not connected
 * @property Connecting connection in progress
 * @property Connected connection successful
 */
enum class ConnectionState {

    /**
     * not connected
     */
    Disconnected,

    /**
     * connection in progress
     */
    Connecting,

    /**
     * connection successful
     */
    Connected,
}