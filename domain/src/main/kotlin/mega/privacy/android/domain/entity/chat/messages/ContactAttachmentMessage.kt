package mega.privacy.android.domain.entity.chat.messages

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
    val contactEmail: String,
    val contactUserName: String,
    val contactHandle: Long
) : TypedMessage
