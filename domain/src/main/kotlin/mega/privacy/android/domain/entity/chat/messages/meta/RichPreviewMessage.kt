package mega.privacy.android.domain.entity.chat.messages.meta

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Rich preview message
 *
 * @property content Message content
 * @property chatRichPreviewInfo [ChatRichPreviewInfo]
 * @property isEdited Whether the message has been edited
 */
@Serializable
data class RichPreviewMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val reactions: List<Reaction>,
    val content: String,
    val chatRichPreviewInfo: ChatRichPreviewInfo?,
    val isEdited: Boolean,
) : MetaMessage