package mega.privacy.android.domain.entity.chat.messages

import mega.privacy.android.domain.entity.chat.ChatMessageStatus

/**
 * Voice clip message
 */
data class VoiceClipMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    val name: String,
    val size: Long,
    val duration: Int,
    val status: ChatMessageStatus,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val shouldShowDate: Boolean,
) : TypedMessage
