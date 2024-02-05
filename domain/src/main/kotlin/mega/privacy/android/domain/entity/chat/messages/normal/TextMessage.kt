package mega.privacy.android.domain.entity.chat.messages.normal

import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Text message
 *
 * @param content Message content.
 * @param hasOtherLink Whether the message contains other links. (Not contact link, file link, folder link)
 */
data class TextMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val shouldShowDate: Boolean,
    override val reactions: List<Reaction>,
    val content: String?,
    val hasOtherLink: Boolean,
) : NormalMessage