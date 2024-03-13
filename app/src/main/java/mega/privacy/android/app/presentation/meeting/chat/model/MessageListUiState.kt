package mega.privacy.android.app.presentation.meeting.chat.model

import mega.privacy.android.domain.entity.user.UserUpdate

/**
 * Message list ui state
 *
 * @property lastSeenMessageId
 * @property isJumpingToLastSeenMessage
 * @property userUpdate
 * @property receivedMessages Set of received messages
 * @property extraUnreadCount the number of unread message after opening the chat room
 */
data class MessageListUiState(
    val lastSeenMessageId: Long = -1L,
    val isJumpingToLastSeenMessage: Boolean = false,
    val userUpdate: UserUpdate? = null,
    val receivedMessages: Set<Long> = emptySet(),
    val extraUnreadCount: Int = 0
)