package mega.privacy.android.domain.entity.chat.messages.invalid

/**
 * Format invalid message
 *
 * @property msgId
 * @property time
 * @property isMine
 * @property userHandle
 */
data class FormatInvalidMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long
) : InvalidMessage