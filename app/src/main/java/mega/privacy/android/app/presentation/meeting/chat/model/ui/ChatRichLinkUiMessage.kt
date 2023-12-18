package mega.privacy.android.app.presentation.meeting.chat.model.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.meeting.chat.view.ChatAvatar
import mega.privacy.android.app.presentation.meeting.chat.view.message.meta.richlink.ChatRichLinkMessageView
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage

/**
 * Chat rich link ui message
 *
 * @property message
 * @property showDate
 * @property showAvatar
 * @property showTime
 */
data class ChatRichLinkUiMessage(
    override val message: RichPreviewMessage,
    override val showDate: Boolean,
    override val showAvatar: Boolean,
    override val showTime: Boolean,
) : UiChatMessage {
    override val contentComposable: @Composable (RowScope.() -> Unit) = {
        ChatRichLinkMessageView(
            modifier = Modifier.weight(1f),
            isMe = message.isMine,
            preview = message.preview,
        )
    }

    override val avatarComposable: @Composable (RowScope.() -> Unit) = {
        if (showAvatar) {
            ChatAvatar(modifier = Modifier.align(Alignment.Bottom), handle = message.userHandle)
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
    }

    override val modifier: Modifier
        get() = if (message.isMine) {
            Modifier
                .padding(start = 8.dp)
                .fillMaxWidth()
        } else {
            Modifier
                .padding(end = 8.dp)
                .fillMaxWidth()
        }
}