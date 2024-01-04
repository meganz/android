package mega.privacy.android.app.presentation.meeting.chat.model.messages.normal

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.extension.canForward
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.normal.ChatMessageTextView
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage

/**
 * Text u i chat message
 *
 * @property message Text message
 */
data class TextUiMessage(
    val message: TextMessage,
    override val showAvatar: Boolean,
    override val showTime: Boolean,
    override val showDate: Boolean,
) : AvatarMessage() {
    override val contentComposable: @Composable (RowScope.() -> Unit) = {
        ChatMessageTextView(text = message.content.orEmpty(), isMe = message.isMine)
    }

    override val displayAsMine = message.isMine
    override val canForward = message.canForward
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}