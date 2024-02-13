package mega.privacy.android.domain.entity.chat.messages.meta

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Giphy message
 *
 * @property chatGifInfo [ChatGifInfo]
 */
@Serializable
data class GiphyMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val reactions: List<Reaction>,
    val chatGifInfo: ChatGifInfo?,
) : MetaMessage