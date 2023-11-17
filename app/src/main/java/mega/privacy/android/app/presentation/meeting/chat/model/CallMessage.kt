package mega.privacy.android.app.presentation.meeting.chat.model

import mega.privacy.android.domain.entity.chat.ChatMessageTermCode

/**
 * Call message
 *
 */
sealed interface CallMessage : UiChatMessage

/**
 * Call started message
 *
 */
data class CallStartedMessage(
    override val time: Long,
    override val isMe: Boolean,
) : CallMessage

/**
 * Call ended message
 *
 * @property termCode Reason of the call
 * @property duration Duration of the call
 */
data class CallEndedMessage(
    override val time: Long,
    override val isMe: Boolean,
    val termCode: ChatMessageTermCode,
    val duration: Long,
) : CallMessage