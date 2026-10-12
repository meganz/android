package mega.privacy.android.domain.entity.chat

/**
 * Event emitted while the node history of a chat, the messages carrying attached nodes, is open.
 */
sealed interface ChatNodeHistoryEvent {

    /**
     * The node history is open and attachments can be requested. Emitted to every collector,
     * possibly more than once, so handle it idempotently.
     */
    data object HistoryOpened : ChatNodeHistoryEvent

    /**
     * An attachment message loaded from the history, delivered from newest to oldest.
     *
     * @property message The loaded [ChatMessage], with every node it carries.
     */
    data class AttachmentLoaded(val message: ChatMessage) : ChatNodeHistoryEvent

    /**
     * The requested batch of attachment messages has been delivered, or no more history is
     * available from the reported source.
     */
    data object LoadBatchFinished : ChatNodeHistoryEvent

    /**
     * A new attachment message received in the chat.
     *
     * @property message The received [ChatMessage], with every node it carries.
     */
    data class AttachmentReceived(val message: ChatMessage) : ChatNodeHistoryEvent

    /**
     * An attachment message deleted from the chat.
     *
     * @property messageId Id of the deleted message.
     */
    data class AttachmentDeleted(val messageId: Long) : ChatNodeHistoryEvent

    /**
     * The node history truncated.
     *
     * @property messageId Id of the message from which the history has been truncated, or an
     * invalid handle if no messages are left.
     */
    data class HistoryTruncated(val messageId: Long) : ChatNodeHistoryEvent
}
