package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ChatLinkCreatedView
import mega.privacy.android.domain.entity.chat.messages.management.ChatLinkCreatedMessage


/**
 * Chat link created UI message.
 */
data class ChatLinkCreatedUiMessage(
    override val message: ChatLinkCreatedMessage,
    override val showDate: Boolean,
) : UiChatMessage {

    override val contentComposable: @Composable (RowScope.() -> Unit) = {
        ChatLinkCreatedView(
            message = message,
            modifier = Modifier.padding(start = 32.dp)
        )
    }

    override val avatarComposable: @Composable (RowScope.() -> Unit)? = null

    override val showAvatar: Boolean = false

    override val showTime: Boolean = true
}
