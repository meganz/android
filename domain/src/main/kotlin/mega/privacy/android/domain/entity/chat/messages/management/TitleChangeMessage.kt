package mega.privacy.android.domain.entity.chat.messages.management

/**
 * Title change message
 */
data class TitleChangeMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
) : ManagementMessage