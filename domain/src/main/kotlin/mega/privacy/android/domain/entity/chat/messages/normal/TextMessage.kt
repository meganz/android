package mega.privacy.android.domain.entity.chat.messages.normal

/**
 * Text message
 */
data class TextMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    val content: String?
) : NormalMessage