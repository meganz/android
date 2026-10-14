package mega.privacy.android.domain.entity.chat

import mega.privacy.android.domain.entity.chat.messages.NodeAttachmentMessage

/**
 * A page of the node history of a chat.
 *
 * @property messages The loaded attachment messages, from newest to oldest. Each message gives
 * only the file of its first node, because an attachment message is expected to carry one node.
 * @property hasMore Whether older node history can still be loaded.
 */
data class ChatNodeHistoryPage(
    val messages: List<NodeAttachmentMessage>,
    val hasMore: Boolean,
)
