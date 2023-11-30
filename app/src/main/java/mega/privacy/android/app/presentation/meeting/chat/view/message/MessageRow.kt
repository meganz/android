package mega.privacy.android.app.presentation.meeting.chat.view.message

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.meeting.chat.extension.canForward
import mega.privacy.android.app.presentation.meeting.chat.model.ui.UiChatMessage
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.core.ui.controls.chat.ChatMessageContainer
import mega.privacy.android.domain.entity.chat.messages.management.ManagementMessage

/**
 * Message row
 *
 * @param uiChatMessage
 * @param modifier
 * @param showTime true if the message should show time otherwise false
 * @param hasAvatar true if the message has avatar otherwise false
 * @param showAvatar true if the message should show avatar otherwise false
 */
@Composable
fun MessageRow(
    uiChatMessage: UiChatMessage,
    modifier: Modifier = Modifier,
    showTime: Boolean = true,
    hasAvatar: Boolean = true,
    showAvatar: Boolean = true,
) {
    val isManagementMessage = uiChatMessage.message is ManagementMessage
    val context = LocalContext.current
    ChatMessageContainer(
        modifier = modifier,
        // all message content align left should be treat as other's message (Management, ...)
        isMine = uiChatMessage.message.isMine && !isManagementMessage,
        showForwardIcon = uiChatMessage.message.canForward,
        time = if (showTime) TimeUtils.formatDate(
            uiChatMessage.message.time,
            TimeUtils.DATE_SHORT_FORMAT,
            context,
        ) else null,
        avatarOrIcon = {
            if (hasAvatar) {
                if (showAvatar) {
                    // replace with avatar size 24 dp next MR
                    Spacer(modifier = Modifier.size(24.dp))
                } else {
                    // we don't show avatar but we still need to reserve space for it
                    Spacer(modifier = Modifier.size(24.dp))
                }
            }
        },
    ) {
        uiChatMessage.contentComposable()
    }
}