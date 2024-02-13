package mega.privacy.android.domain.entity.chat.messages.management

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Chat link created message
 */
@Serializable
data class ChatLinkCreatedMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val reactions: List<Reaction>,
) : ManagementMessage