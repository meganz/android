package mega.privacy.android.domain.entity.chat.messages

/**
 * Node attachment message
 */
data class NodeAttachmentMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
) : TypedMessage
