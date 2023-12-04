package mega.privacy.android.domain.entity.chat.messages.management

/**
 * Private mode set message
 */
data class PrivateModeSetMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
) : ManagementMessage