package mega.privacy.android.app.presentation.meeting.chat

import mega.privacy.android.domain.entity.contacts.UserChatStatus

/**
 * Chat ui state
 *
 * @property title title of the chat
 * @property isChatNotificationMute whether notification is mute
 * @property isPrivateChat whether the chat is private
 * @property userChatStatus User chat status if is a 1to1 conversation, null otherwise.
 */
data class ChatUiState(
    val title: String? = null,
    val isChatNotificationMute: Boolean = false,
    val isPrivateChat: Boolean? = null,
    val userChatStatus: UserChatStatus? = null,
)