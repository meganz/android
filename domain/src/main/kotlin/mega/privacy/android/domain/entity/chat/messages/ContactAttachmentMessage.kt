package mega.privacy.android.domain.entity.chat.messages

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Contact attachment message
 * @property contactEmail Contact email
 * @property contactUserName Contact name
 * @property contactHandle Contact handle
 */
@Serializable
data class ContactAttachmentMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val reactions: List<Reaction>,
    val contactEmail: String,
    val contactUserName: String,
    val contactHandle: Long,
) : TypedMessage
