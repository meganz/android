package mega.privacy.android.app.presentation.meeting.chat.model.messages.meta

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.extension.canForward
import mega.privacy.android.app.presentation.meeting.chat.extension.canLongClick
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.meta.GiphyMessageView
import mega.privacy.android.app.utils.GiphyUtil
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage

private const val MAX_SIZE_FOR_AUTO_PLAY = 1024 * 1024 * 4  // 4MB

/**
 * Ui message for Giphy message
 *
 * @property message
 * @property showDate
 * @property showAvatar
 * @property showTime
 */
class ChatGiphyUiMessage(
    val message: GiphyMessage,
) : AvatarMessage() {
    @OptIn(ExperimentalFoundationApi::class)
    override val contentComposable: @Composable (RowScope.() -> Unit) = {
        message.chatGifInfo?.let { giphy ->
            GiphyMessageView(
                url = giphy.webpSrc?.let { GiphyUtil.getOriginalGiphySrc(it) }?.toString() ?: "",
                width = giphy.width,
                height = giphy.height,
                title = giphy.title,
                autoPlayGif = if (autoPlay) true else giphy.webpSize < MAX_SIZE_FOR_AUTO_PLAY,
                onLoaded = { autoPlay = true },
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { longClick?.let { it(message) } }
                ),
            )
        }
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
    private var autoPlay: Boolean = false
}
