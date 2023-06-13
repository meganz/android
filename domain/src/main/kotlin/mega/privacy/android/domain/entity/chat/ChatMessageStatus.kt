package mega.privacy.android.domain.entity.chat

/**
 * Status of the message.
 */
enum class ChatMessageStatus {
    /**
     * Unknown status.
     */
    UNKNOWN,

    /**
     * Sending status.
     */
    SENDING,

    /**
     * Sending manual status.
     * The user can whether manually retry to send the message (get content and send new message as
     * usual through MegaChatApi::sendMessage), or discard the message.
     * In both cases, the message should be removed from the manual-send queue by calling
     * MegaChatApi::removeUnsentMessage once the user has sent or discarded it.
     */
    SENDING_MANUAL,

    /**
     * Server received status.
     */
    SERVER_RECEIVED,

    /**
     * Server rejected status.
     */
    SERVER_REJECTED,

    /**
     * Server delivered status.
     */
    DELIVERED,

    /**
     * Not sent status.
     */
    NOT_SENT,

    /**
     * Seen status.
     */
    SEEN
}