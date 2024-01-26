package mega.privacy.android.domain.entity.chat.messages.management

/**
 * Call started message
 */
data class CallStartedMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val shouldShowDate: Boolean,
) : CallMessage