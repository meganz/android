package mega.privacy.android.app.presentation.meeting.chat.model.messages.meta

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.extension.canForward
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.meta.GiphyMessageView
import mega.privacy.android.app.utils.GiphyUtil
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage

class ChatGiphyUiMessage(
    val message: GiphyMessage,
    override val showDate: Boolean,
    override val showAvatar: Boolean,
    override val showTime: Boolean,
) : AvatarMessage() {
    override val contentComposable: @Composable (RowScope.() -> Unit) = {
        message.giphy?.let { giphy ->
            GiphyMessageView(
                url = giphy.webpSrc?.let { GiphyUtil.getOriginalGiphySrc(it) }?.toString() ?: "",
                width = giphy.width,
                height = giphy.height,
                title = giphy.title
            )
        }
    }
    override val displayAsMine = message.isMine
    override val canForward = message.canForward
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}