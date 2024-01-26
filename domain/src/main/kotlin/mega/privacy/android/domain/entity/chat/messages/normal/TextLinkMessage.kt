package mega.privacy.android.domain.entity.chat.messages.normal

import mega.privacy.android.domain.entity.chat.LinkDetail

/**
 * Contact link message
 * @property links all links in the message
 * @property content Link
 */
data class TextLinkMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val tempId: Long,
    val links: List<LinkDetail>,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val shouldShowDate: Boolean,
    val content: String,
) : NormalMessage