package mega.privacy.android.app.presentation.meeting.chat.model.messages.normal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.extension.canForward
import mega.privacy.android.app.presentation.meeting.chat.extension.canLongClick
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.link.ChatLinksMessageView
import mega.privacy.android.domain.entity.chat.messages.normal.TextLinkMessage

/**
 * Contact link ui message
 * @property message Contact link message
 *
 */
data class TextLinkUiMessage(
    val message: TextLinkMessage,
) : AvatarMessage() {
    @OptIn(ExperimentalFoundationApi::class)
    override val contentComposable: @Composable (RowScope.() -> Unit) = {
        ChatLinksMessageView(
            modifier = Modifier
                .weight(1f, fill = false)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { longClick?.let { it(message) } }
                ),
            message = message
        )
    }
    override val showAvatar = message.shouldShowAvatar
    override val showTime = message.shouldShowTime
    override val showDate = message.shouldShowDate
    override val displayAsMine = message.isMine
    override val canForward = message.canForward
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val canLongClick = message.canLongClick
    override val id = message.msgId
}