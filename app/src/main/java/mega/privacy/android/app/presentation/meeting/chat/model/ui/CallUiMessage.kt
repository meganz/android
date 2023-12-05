package mega.privacy.android.app.presentation.meeting.chat.model.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.view.message.ChatCallMessageView
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
    val isOneToOneChat: Boolean,
) : UiChatMessage {
    override val contentComposable: @Composable (RowScope.() -> Unit) = {
        ChatCallMessageView(message = message, isOneToOneChat = isOneToOneChat)
    }

    override val avatarComposable: @Composable (RowScope.() -> Unit)? = null

    override val showAvatar: Boolean = false

    override val showTime: Boolean = true
}