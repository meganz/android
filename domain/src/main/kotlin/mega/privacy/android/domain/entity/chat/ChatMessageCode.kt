package mega.privacy.android.domain.entity.chat

/**
 * Chat message code.
 * Generic code used for different purposes.
 */
enum class ChatMessageCode {
    /**
     * Decrypting code.
     * Message pending to be decrypted
     */
    DECRYPTING,

    /**
     * Invalid key code.
     * Key not found for the message (permanent failure)
     */
    INVALID_KEY,

    /**
     * Invalid signature code.
     * Signature verification failure (permanent failure)
     */
    INVALID_SIGNATURE,

    /**
     * Invalid format code.
     * Malformed/corrupted data in the message (permanent failure)
     */
    INVALID_FORMAT,

    /**
     * Invalid type code.
     * Management message of unknown type (transient, not supported by the app yet)
     */
    INVALID_TYPE,

    /**
     * Group chat participants have changed.
     * Only for [ChatMessageStatus.SENDING_MANUAL].
     */
    REASON_PEERS_CHANGED,

    /**
     * Message is too old to auto-retry sending.
     * Only for [ChatMessageStatus.SENDING_MANUAL].
     */
    REASON_TOO_OLD,

    /**
     * chatd rejected the message, for unknown reason.
     * Only for [ChatMessageStatus.SENDING_MANUAL].
     */
    REASON_GENERAL_REJECT,

    /**
     * Read-only privilege or not belong to the chatroom.
     * Only for [ChatMessageStatus.SENDING_MANUAL].
     */
    REASON_NO_WRITE_ACCESS,

    /**
     * dited message has the same content than original message.
     * Only for [ChatMessageStatus.SENDING_MANUAL].
     */
    REASON_NO_CHANGES,

    /**
     * Unknown code.
     */
    UNKNOWN,
}