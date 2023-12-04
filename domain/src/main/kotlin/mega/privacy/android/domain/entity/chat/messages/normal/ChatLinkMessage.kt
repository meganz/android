package mega.privacy.android.domain.entity.chat.messages.normal

/**
 * Chat link message
 */
data class ChatLinkMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
) : NormalMessage