package mega.privacy.android.app.presentation.meeting.chat.view.message

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import mega.privacy.android.app.presentation.meeting.chat.extension.canForward
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.core.ui.controls.chat.ChatMessageContainer
import mega.privacy.android.domain.entity.chat.messages.management.ManagementMessage

/**
 * Message row
 *
 * @param uiChatMessage
 * @param modifier
 */
@Composable
fun MessageRow(
    uiChatMessage: UiChatMessage,
    modifier: Modifier = Modifier,
) {
    val isManagementMessage = uiChatMessage.message is ManagementMessage
    val context = LocalContext.current
    ChatMessageContainer(
        modifier = modifier,
        // all message content align left should be treat as other's message (Management, ...)
        isMine = uiChatMessage.message.isMine && !isManagementMessage,
        showForwardIcon = uiChatMessage.message.canForward,
        time = if (uiChatMessage.showTime) TimeUtils.formatTime(
            uiChatMessage.message.time,
        ) else null,
        date = if (uiChatMessage.showDate) TimeUtils.formatDate(
            uiChatMessage.message.time,
            TimeUtils.DATE_SHORT_FORMAT,
            context,
        ) else null,
        avatarOrIcon = uiChatMessage.avatarComposable,
        content = uiChatMessage.contentComposable,
    )
}