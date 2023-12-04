package mega.privacy.android.domain.entity.chat.messages.management

/**
 * Chat link created message
 */
data class ChatLinkCreatedMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
) : ManagementMessage