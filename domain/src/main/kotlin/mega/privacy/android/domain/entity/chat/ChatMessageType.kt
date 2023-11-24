package mega.privacy.android.domain.entity.chat

/**
 * Chat message type.
 */
enum class ChatMessageType {
    /**
     * Unknown type.
     * Should be ignored/hidden. The ChatMessage.code can take the following values:
     * - ChatMessageCode.INVALID_TYPE
     * - ChatMessageCode.INVALID_KEYID
     * - ChatMessageCode.DECRYPTING
     */
    UNKNOWN,

    /**
     * Invalid type.
     * In those cases, the ChatMessage.code can take the following values:
     *  - ChatMessageCode.INVALID_FORMAT
     *  - ChatMessageCode.INVALID_SIGNATURE
     */
    INVALID,

    /**
     * Normal type.
     * Regular text message.
     */
    NORMAL,

    /**
     * Alter participants type.
     * Management message indicating the participants in the chat have changed.
     */
    ALTER_PARTICIPANTS,

    /**
     * Truncate type.
     * Management message indicating the history of the chat has been truncated.
     */
    TRUNCATE,

    /**
     * Privilege change type.
     * Management message indicating the privilege level of a user has changed.
     */
    PRIV_CHANGE,

    /**
     * Chat title type.
     * Management message indicating the title of the chat has changed.
     */
    CHAT_TITLE,

    /**
     * Call ended type.
     */
    CALL_ENDED,

    /**
     * Call started type.
     */
    CALL_STARTED,

    /**
     * Public handle create type.
     */
    PUBLIC_HANDLE_CREATE,

    /**
     * Public handle delete type.
     */
    PUBLIC_HANDLE_DELETE,

    /**
     * Set private mode type.
     */
    SET_PRIVATE_MODE,

    /**
     * Set retention time type.
     */
    SET_RETENTION_TIME,

    /**
     * Scheduled meeting type.
     */
    SCHED_MEETING,

    /**
     * Node attachment type.
     * User message including info about a shared node.
     */
    NODE_ATTACHMENT,

    /**
     * Revoke node attachment type.
     * User message including info about a node that has stopped being shared.
     */
    REVOKE_NODE_ATTACHMENT,

    /**
     * Contact attachment type.
     */
    CONTACT_ATTACHMENT,

    /**
     * Contains meta type.
     */
    CONTAINS_META,

    /**
     * Voice clip type.
     * User messages including info about a node that represents a voice-clip.
     */
    VOICE_CLIP
}