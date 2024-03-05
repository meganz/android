package mega.privacy.android.domain.entity.chat.messages

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction

/**
 * Contact attachment message
 * @property contactEmail Contact email
 * @property contactUserName Contact name
 * @property contactHandle Contact handle
 * @property status Status
 * @property isMe True if the contact is me
 * @property isContact True if it is a contact, false otherwise
 */
@Serializable
data class ContactAttachmentMessage(
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
    override val content: String?,
    val contactEmail: String,
    val contactUserName: String,
    val contactHandle: Long,
    val isMe: Boolean,
    val isContact: Boolean,
) : TypedMessage
