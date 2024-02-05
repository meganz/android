package mega.privacy.android.domain.entity.chat.messages

import mega.privacy.android.domain.entity.chat.messages.reactions.MessageReaction

/**
 * Contact attachment message
 * @property contactEmail Contact email
 * @property contactUserName Contact name
 * @property contactHandle Contact handle
 */
data class ContactAttachmentMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val shouldShowDate: Boolean,
    override val reactions: List<MessageReaction>,
    val contactEmail: String,
    val contactUserName: String,
    val contactHandle: Long,
) : TypedMessage
