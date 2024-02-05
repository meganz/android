package mega.privacy.android.app.presentation.meeting.chat.model.messages.meta

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.meeting.chat.extension.canForward
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.meta.ChatRichLinkMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
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
    private val message: RichPreviewMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun RowScope.ContentComposable(onLongClick: (TypedMessage) -> Unit) {
        ChatRichLinkMessageView(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { onLongClick(message) }
                ),
            isMe = message.isMine,
            preview = message.chatRichPreviewInfo,
        )
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

    override val showAvatar = message.shouldShowAvatar
    override val showTime = message.shouldShowTime
    override val showDate = message.shouldShowDate
    override val displayAsMine = message.isMine
    override val canForward = message.canForward
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}