package mega.privacy.android.app.presentation.meeting.chat.model.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.meeting.chat.view.ChatAvatar
import mega.privacy.android.app.presentation.meeting.chat.view.message.ChatMessageTextView
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage

/**
 * Text u i chat message
 *
 * @property message Text message
 */
data class TextUiMessage(
    override val message: TextMessage,
    override val showAvatar: Boolean = true,
) : UiChatMessage {
    override val contentComposable: @Composable (RowScope.() -> Unit) = {
        ChatMessageTextView(text = message.content.orEmpty(), isMe = message.isMine)
    }

    override val avatarComposable: @Composable RowScope.() -> Unit = {
        if (showAvatar) {
            ChatAvatar(modifier = Modifier.align(Alignment.Bottom), handle = message.userHandle)
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}