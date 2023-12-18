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
    val content: String,
) : ManagementMessage