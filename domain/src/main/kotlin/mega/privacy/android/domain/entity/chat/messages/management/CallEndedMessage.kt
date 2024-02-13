package mega.privacy.android.domain.entity.chat.messages.management

import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.chat.ChatMessageTermCode
import mega.privacy.android.domain.entity.chat.messages.reactions.Reaction
import kotlin.time.Duration

/**
 * Call ended message
 *
 * @property termCode Reason of the call termination
 * @property duration Duration of the call
 */
@Serializable
data class CallEndedMessage(
    override val chatId: Long,
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    override val shouldShowAvatar: Boolean,
    override val shouldShowTime: Boolean,
    override val reactions: List<Reaction>,
    val termCode: ChatMessageTermCode,
    val duration: Duration,
) : CallMessage