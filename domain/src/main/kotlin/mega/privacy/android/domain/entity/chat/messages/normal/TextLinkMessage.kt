package mega.privacy.android.domain.entity.chat.messages.normal

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.LinkDetail
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Contact link message
 *
 * @property links all links in the message
 */
@Serializable
data class TextLinkMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val reactions: List<Reaction>,
    override val content: String,
    val links: List<LinkDetail>,
) : NormalMessage