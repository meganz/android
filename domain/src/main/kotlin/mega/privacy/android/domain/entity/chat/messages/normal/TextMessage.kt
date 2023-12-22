package mega.privacy.android.domain.entity.chat.messages.normal

/**
 * Text message
 *
 * @param content Message content.
 */
data class TextMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val tempId: Long,
    val content: String?,
) : NormalMessage