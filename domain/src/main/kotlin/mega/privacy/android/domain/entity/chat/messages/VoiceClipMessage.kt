package mega.privacy.android.domain.entity.chat.messages

/**
 * Voice clip message
 */
data class VoiceClipMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean
) : TypedMessage
