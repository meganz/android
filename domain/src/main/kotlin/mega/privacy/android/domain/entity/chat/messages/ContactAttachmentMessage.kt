package mega.privacy.android.domain.entity.chat.messages

/**
 * Contact attachment message
 */
data class ContactAttachmentMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean
) : TypedMessage
