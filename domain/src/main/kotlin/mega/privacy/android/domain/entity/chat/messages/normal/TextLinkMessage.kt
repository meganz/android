package mega.privacy.android.domain.entity.chat.messages.normal

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.LinkDetail
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Text link message
 *
 * @property chatId
 * @property msgId
 * @property time
 * @property isDeletable
 * @property isMine
 * @property userHandle
 * @property shouldShowAvatar
 * @property reactions
 * @property content
 * @property links
 * @property status
 */
@Serializable
data class TextLinkMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isDeletable: Boolean,
    override val isEditable: Boolean,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val reactions: List<Reaction>,
    override val status: ChatMessageStatus,
    override val content: String,
    override val rowId: Long,
    override val isEdited: Boolean,
    val links: List<LinkDetail>,
) : NormalMessage