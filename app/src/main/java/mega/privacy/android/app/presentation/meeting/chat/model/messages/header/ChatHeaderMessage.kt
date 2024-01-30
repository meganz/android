package mega.privacy.android.app.presentation.meeting.chat.model.messages.header

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.FirstMessageHeader
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Chat header message
 */
class ChatHeaderMessage : UiChatMessage {

    @Composable
    override fun MessageListItem(
        uiState: ChatUiState,
        lastUpdatedCache: Long,
        timeFormatter: (Long) -> String,
        dateFormatter: (Long) -> String,
        onLongClick: (TypedMessage) -> Unit,
    ) {
        FirstMessageHeader(uiState.title, uiState.scheduledMeeting)
    }

    override val id = -1L
    override val displayAsMine = false
    override val canForward = false
    override val timeSent = null
    override val userHandle = -1L
    override val showTime = false
    override val showDate = false
}