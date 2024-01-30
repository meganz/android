package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.extension.canForward
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ChatCallMessageView
import mega.privacy.android.core.ui.controls.chat.ChatMessageContainer
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.management.CallMessage

/**
 * Call ui chat message
 *
 * @property message
 */
data class CallUiMessage(
    private val message: CallMessage,
) : UiChatMessage {

    override val showDate = message.shouldShowDate

    override val showTime: Boolean = true

    override val displayAsMine = false

    @Composable
    override fun MessageListItem(
        uiState: ChatUiState,
        lastUpdatedCache: Long,
        timeFormatter: (Long) -> String,
        dateFormatter: (Long) -> String,
        onLongClick: (TypedMessage) -> Unit,
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

    override val canForward = message.canForward
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}