package mega.privacy.android.domain.entity.chat.messages.management

import mega.privacy.android.domain.entity.chat.ChatMessageTermCode

/**
 * Call ended message
 *
 * @property termCode Reason of the call termination
 * @property duration Duration of the call
 */
data class CallEndedMessage(
    override val msgId: Long,
    override val time: Long,
    override val isMine: Boolean,
    override val userHandle: Long,
    val termCode: ChatMessageTermCode,
    val duration: Long,
) : CallMessage