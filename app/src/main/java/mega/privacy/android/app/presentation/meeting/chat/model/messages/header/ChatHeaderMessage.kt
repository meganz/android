package mega.privacy.android.app.presentation.meeting.chat.model.messages.header

import android.content.Context
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.FirstMessageHeader

/**
 * Chat header message
 */
class ChatHeaderMessage : UiChatMessage {

    @Composable
    override fun MessageListItem(uiState: ChatUiState, context: Context) {
        FirstMessageHeader(uiState.title, uiState.scheduledMeeting)
    }

    override val id: Long
        get() = -1L
    override val contentComposable: @Composable() (RowScope.() -> Unit)
        get() = {}
    override val avatarComposable: @Composable() (RowScope.() -> Unit)?
        get() = null
    override val displayAsMine: Boolean
        get() = false
    override val canForward: Boolean
        get() = false
    override val timeSent: Long? = null
    override val userHandle: Long? = null
    override val showAvatar: Boolean
        get() = false
    override val showTime: Boolean
        get() = false
    override val showDate: Boolean
        get() = false

}