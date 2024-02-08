package mega.privacy.android.app.presentation.meeting.chat.model

/**
 * Message list ui state
 *
 * @property lastSeenMessageId Last seen message id
 * @property isJumpingToLastSeenMessage Is jumping to last seen message
 */
data class MessageListUiState(
    val lastSeenMessageId: Long = -1L,
    val isJumpingToLastSeenMessage: Boolean = false,
)