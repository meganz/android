package mega.privacy.android.app.presentation.meeting.chat.model.ui

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.view.message.ChatMessageTextView
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage

/**
 * Text u i chat message
 *
 * @property message Text message
 */
data class TextUiMessage(
    override val message: TextMessage,
) : UiChatMessage {
    override val contentComposable: @Composable () -> Unit = {
        ChatMessageTextView(text = message.content.orEmpty(), isMe = message.isMine)
    }
}