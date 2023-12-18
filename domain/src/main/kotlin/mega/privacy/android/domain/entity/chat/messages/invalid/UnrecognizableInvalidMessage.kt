package mega.privacy.android.domain.entity.chat.messages.invalid

/**
 * Unrecognizable invalid message
 *
 * @property msgId
 * @property time
 * @property isMine
 * @property userHandle
 */
data class UnrecognizableInvalidMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long
) : InvalidMessage