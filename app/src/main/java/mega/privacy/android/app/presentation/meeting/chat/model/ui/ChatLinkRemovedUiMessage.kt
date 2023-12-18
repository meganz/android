package mega.privacy.android.app.presentation.meeting.chat.model.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ChatLinkRemovedView
import mega.privacy.android.domain.entity.chat.messages.management.ChatLinkRemovedMessage


/**
 * Chat link removed UI message.
 */
data class ChatLinkRemovedUiMessage(
    override val message: ChatLinkRemovedMessage,
    override val showDate: Boolean,
) : UiChatMessage {

    override val contentComposable: @Composable (RowScope.() -> Unit) = {
        ChatLinkRemovedView(
            message = message,
            modifier = Modifier.padding(start = 32.dp)
        )
    }

    override val avatarComposable: @Composable (RowScope.() -> Unit)? = null

    override val showAvatar: Boolean = false

    override val showTime: Boolean = true
}
