package mega.privacy.android.domain.entity.chat.messages

/**
 * Revoke node attachment message
 */
data class RevokeNodeAttachmentMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean
) : TypedMessage
