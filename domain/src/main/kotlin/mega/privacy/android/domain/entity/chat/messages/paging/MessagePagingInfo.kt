package mega.privacy.android.domain.entity.chat.messages.paging

/**
 * Message paging info
 *
 * @property userHandle User handle
 * @property isMine True if the message is mine.
 * @property timestamp Timestamp
 */
data class MessagePagingInfo(
    val userHandle: Long,
    val isMine: Boolean,
    val timestamp: Long,
)
