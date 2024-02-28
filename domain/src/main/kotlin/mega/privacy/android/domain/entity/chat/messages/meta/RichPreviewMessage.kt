package mega.privacy.android.domain.entity.chat.messages.meta

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Rich preview message
 *
 * @property chatId
 * @property msgId
 * @property time
 * @property isDeletable
 * @property isMine
 * @property userHandle
 * @property shouldShowAvatar
 * @property shouldShowTime
 * @property reactions
 * @property content
 * @property chatRichPreviewInfo
 * @property isEdited
 * @property status
 */
@Serializable
data class RichPreviewMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isDeletable: Boolean,
    override val isEditable: Boolean,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val reactions: List<Reaction>,
    override val status: ChatMessageStatus,
    override val content: String,
    val chatRichPreviewInfo: ChatRichPreviewInfo?,
    val isEdited: Boolean,
) : MetaMessage