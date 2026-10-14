package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage

/**
 * Event emitted while the node history of a chat is open, with each attachment message given as
 * the file of its first node, because an attachment message is expected to carry one node.
 */
sealed interface ChatNodeHistoryFileEvent {

    /**
     * The node history is open and pages can be loaded. Emitted to every collector, possibly
     * more than once, so handle it idempotently.
     */
    data object HistoryOpened : ChatNodeHistoryFileEvent

    /**
     * A new attachment message received in the chat.
     *
     * @property message The received [NodeAttachmentMessage].
     */
    data class FileReceived(val message: NodeAttachmentMessage) : ChatNodeHistoryFileEvent

    /**
     * An attachment message deleted from the chat.
     *
     * @property messageId Id of the deleted message.
     */
    data class FileDeleted(val messageId: Long) : ChatNodeHistoryFileEvent

    /**
     * The node history truncated.
     *
     * @property messageId Id of the message from which the history has been truncated, or an
     * invalid handle if no messages are left.
     */
    data class HistoryTruncated(val messageId: Long) : ChatNodeHistoryFileEvent
}
