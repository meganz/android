package mega.privacy.android.domain.entity.chat

/**
 * Chat load history status
 */
enum class ChatHistoryLoadStatus {

    /**
     * History has to be fetched from server, but we are not logged in yet
     */
    ERROR,

    /**
     * There's no more history available (not even in the server)
     */
    NONE,

    /**
     * Messages will be fetched locally (RAM or DB)
     */
    LOCAL,

    /**
     * Messages will be requested to the server. Expect some delay
     */
    REMOTE,
}