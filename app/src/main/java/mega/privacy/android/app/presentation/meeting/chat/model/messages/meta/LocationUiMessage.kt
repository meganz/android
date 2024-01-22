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
import mega.privacy.android.app.presentation.meeting.chat.extension.canLongClick
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.meta.ChatLocationMessageView
import mega.privacy.android.domain.entity.chat.messages.meta.LocationMessage

/**
 * Location ui message
 */
class LocationUiMessage(
    val message: LocationMessage,
) : AvatarMessage() {
    @OptIn(ExperimentalFoundationApi::class)
    override val contentComposable: @Composable (RowScope.() -> Unit) = {
        ChatLocationMessageView(
            message = message,
            modifier = Modifier
                .weight(weight = 1f, fill = false)
                .combinedClickable(
                    onClick = {},
                    onLongClick = { longClick?.let { it(message) } }
                )
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
    override val canLongClick = message.canLongClick
    override val id = message.msgId
}
