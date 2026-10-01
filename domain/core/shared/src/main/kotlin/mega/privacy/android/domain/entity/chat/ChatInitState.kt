package mega.privacy.android.domain.entity.chat

/**
 * Chat API init state
 */
enum class ChatInitState {
    /**
     * Invalid chat API init state
     */
    INVALID,

    /**
     * Equivalent of MegaChatApi.INIT_ERROR
     */
    ERROR,

    /**
     * Equivalent of MegaChatApi.INIT_NOT_DONE
     */
    NOT_DONE,

    /**
     * Equivalent of MegaChatApi.INIT_WAITING_NEW_SESSION
     */
    WAITING_NEW_SESSION,

    /**
     * Equivalent of MegaChatApi.INIT_OFFLINE_SESSION
     */
    OFFLINE,

    /**
     * Equivalent of MegaChatApi.INIT_ONLINE_SESSION
     */
    ONLINE,

    /**
     * Equivalent of MegaChatApi.INIT_ANONYMOUS
     */
    ANONYMOUS,

    /**
     * Equivalent of MegaChatApi.INIT_TERMINATED
     */
    TERMINATED,

    /**
     * Equivalent of MegaChatApi.INIT_NO_CACHE
     */
    NO_CACHE
}