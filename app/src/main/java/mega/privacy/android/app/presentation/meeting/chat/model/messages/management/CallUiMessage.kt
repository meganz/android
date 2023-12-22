package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

import android.content.Context
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ChatCallMessageView
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.core.ui.controls.chat.ChatMessageContainer
import mega.privacy.android.domain.entity.chat.messages.management.CallMessage

/**
 * Call ui chat message
 *
 * @property message
 * @property isOneToOneChat
 */
data class CallUiMessage(
    override val message: CallMessage,
    override val showDate: Boolean,
) : ManagementUiChatMessage() {
    override val contentComposable: @Composable (RowScope.() -> Unit) = {

    }

    @Composable
    override fun MessageListItem(uiState: ChatUiState, context: Context) {
        ChatMessageContainer(
            modifier = Modifier.fillMaxWidth(),
            isMine = displayAsMine,
            showForwardIcon = canForward,
            time = getTimeOrNull(this),
            date = getDateOrNull(this, context),
            avatarOrIcon = avatarComposable,
            content = {
                ChatCallMessageView(
                    message = message,
                    isOneToOneChat = !uiState.isGroup && !uiState.isMeeting
                )
            },
        )
    }

    private fun getTimeOrNull(uiChatMessage: UiChatMessage) =
        if (uiChatMessage.showTime) uiChatMessage.timeSent?.let {
            TimeUtils.formatTime(
                it,
            )
        } else null

    private fun getDateOrNull(
        uiChatMessage: UiChatMessage,
        context: Context,
    ) = if (uiChatMessage.showDate) uiChatMessage.timeSent?.let {
        TimeUtils.formatDate(
            it,
            TimeUtils.DATE_SHORT_FORMAT,
            context,
        )
    } else null
}