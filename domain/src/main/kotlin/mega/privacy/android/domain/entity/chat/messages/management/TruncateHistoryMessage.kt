package mega.privacy.android.domain.entity.chat.messages.management

/**
 * Truncate history message
 */
data class TruncateHistoryMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val shouldShowDate: Boolean,
) : ManagementMessage