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
 * @property senderName Sender name.
 * @property senderAvatar Sender avatar.
 * @property senderAvatarColor Default color of the avatar in case [senderAvatar] is null.
 * @property notificationBehaviour [NotificationBehaviour]
 */
data class ChatMessageNotificationData(
    val chat: ChatRoom? = null,
    val msg: ChatMessage,
    val senderName: String? = null,
    val senderAvatar: File? = null,
    val senderAvatarColor: Int? = null,
    val notificationBehaviour: NotificationBehaviour? = null,
)
