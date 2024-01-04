package mega.privacy.android.app.presentation.meeting.chat.model.messages.header

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.FirstMessageHeader

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
    ) {
        FirstMessageHeader(uiState.title, uiState.scheduledMeeting)
    }

    override val id: Long
        get() = -1L
    override val displayAsMine: Boolean
        get() = false
    override val canForward: Boolean
        get() = false
    override val timeSent: Long? = null
    override val userHandle: Long = -1L
    override val showTime: Boolean
        get() = false
    override val showDate: Boolean
        get() = false

}