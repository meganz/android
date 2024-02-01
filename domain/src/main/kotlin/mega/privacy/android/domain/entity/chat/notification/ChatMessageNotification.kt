package mega.privacy.android.domain.entity.chat.notification

import mega.privacy.android.domain.entity.chat.ChatMessage

/**
 * Chat message notification
 *
 * @property chatId
 * @property message
 */
data class ChatMessageNotification(
    val chatId: Long,
    val message: ChatMessage
)
