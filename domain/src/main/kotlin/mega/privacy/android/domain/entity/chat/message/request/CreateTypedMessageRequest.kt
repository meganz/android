package mega.privacy.android.domain.entity.chat.message.request

import mega.privacy.android.domain.entity.chat.ChatMessage

/**
 * Create typed message request
 *
 * @property message [ChatMessage]
 * @property isMine True if the message is mine.
 */
data class CreateTypedMessageRequest(
    val message: ChatMessage,
    val isMine: Boolean,
)