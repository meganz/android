package mega.privacy.android.domain.entity.chat.messages.management

/**
 * Title change message
 * @property content The new title
 */
data class TitleChangeMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val shouldShowDate: Boolean,
    val content: String,
) : ManagementMessage