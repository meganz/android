package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.extension.canForward
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ChatCallMessageView
import mega.privacy.android.core.ui.controls.chat.ChatMessageContainer
import mega.privacy.android.domain.entity.chat.messages.management.CallMessage

/**
 * Call ui chat message
 *
 * @property message
 */
data class CallUiMessage(
    private val message: CallMessage,
    override val showDate: Boolean,
) : UiChatMessage {

    override val showTime: Boolean = true

    override val displayAsMine = false

    @Composable
    override fun MessageListItem(
        uiState: ChatUiState,
        timeFormatter: (Long) -> String,
        dateFormatter: (Long) -> String,
    ) {
        ChatMessageContainer(
            modifier = Modifier.fillMaxWidth(),
            isMine = displayAsMine,
            showForwardIcon = canForward,
            time = getTimeOrNull(timeFormatter),
            date = getDateOrNull(dateFormatter),
            content = {
                ChatCallMessageView(
                    message = message,
                    isOneToOneChat = !uiState.isGroup && !uiState.isMeeting
                )
            },
        )
    }

    override val canForward: Boolean
        get() = message.canForward

    override val timeSent: Long
        get() = message.time

    override val userHandle: Long
        get() = message.userHandle

    override val id: Long
        get() = message.msgId
}