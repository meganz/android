package mega.privacy.android.app.presentation.meeting.chat.model

import mega.privacy.android.domain.entity.user.UserUpdate

/**
 * Message list ui state
 *
 * @property lastSeenMessageId
 * @property isJumpingToLastSeenMessage
 * @property userUpdate
 */
data class MessageListUiState(
    val lastSeenMessageId: Long = -1L,
    val isJumpingToLastSeenMessage: Boolean = false,
    val userUpdate: UserUpdate? = null,
)