package mega.privacy.android.domain.entity.chat.messages.normal

/**
 * File link message
 */
data class FileLinkMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
) : NormalMessage