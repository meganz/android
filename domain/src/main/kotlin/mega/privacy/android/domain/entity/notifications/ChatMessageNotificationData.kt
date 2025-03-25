package mega.privacy.android.domain.entity.notifications

import mega.privacy.android.domain.entity.NotificationBehaviour
import mega.privacy.android.domain.entity.chat.ChatMessage
import mega.privacy.android.domain.entity.chat.ChatRoom
import java.io.File

/**
 * Data class for storing data for a chat message notification.
 *
 * @property chat [ChatRoom]
 * @property msg [ChatMessage]
 */
sealed class ChatMessageNotificationData(open val chat: ChatRoom, open val msg: ChatMessage) {

    /**
     * Data class for storing data for a deleted chat message notification.
     */
    data class DeletedMessage(override val chat: ChatRoom, override val msg: ChatMessage) :
        ChatMessageNotificationData(chat = chat, msg = msg)

    /**
     * Data class for storing data for a seen chat message notification.
     */
    data class SeenMessage(override val chat: ChatRoom, override val msg: ChatMessage) :
        ChatMessageNotificationData(chat = chat, msg = msg)

    /**
     * Data class for storing data for a chat message notification.
     *
     * @property senderName Sender name.
     * @property senderAvatar Sender avatar.
     * @property senderAvatarColor Default color of the avatar in case [senderAvatar] is null.
     * @property notificationBehaviour [NotificationBehaviour]
     */
    data class Message(
        override val chat: ChatRoom,
        override val msg: ChatMessage,
        val senderName: String,
        val senderAvatar: File? = null,
        val senderAvatarColor: Int,
        val notificationBehaviour: NotificationBehaviour,
    ) : ChatMessageNotificationData(msg = msg, chat = chat)
}
