package mega.privacy.android.domain.entity.chat.messages

import mega.privacy.android.domain.entity.chat.ChatMessageStatus
import mega.privacy.android.domain.entity.chat.messages.reactions.MessageReaction
import kotlin.time.Duration

/**
 * Voice clip message
 *
 * @property status Message status
 * @property name name of the voice clip
 * @property size size of the voice clip
 * @property duration duration of the voice clip in milliseconds
 */
data class VoiceClipMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    val status: ChatMessageStatus,
    val name: String,
    val size: Long,
    val duration: Duration,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val shouldShowDate: Boolean,
    override val reactions: List<MessageReaction>,
) : TypedMessage
