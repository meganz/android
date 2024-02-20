package mega.privacy.android.app.presentation.meeting.chat.model.messages.meta

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.meta.GiphyMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage

private const val MAX_SIZE_FOR_AUTO_PLAY = 1024 * 1024 * 4  // 4MB

/**
 * Ui message for Giphy message
 *
 * @property message
 * @property showAvatar
 * @property showTime
 */
class ChatGiphyUiMessage(
    override val message: GiphyMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {

    @Composable
    override fun RowScope.ContentComposable(onLongClick: (TypedMessage) -> Unit) {
        message.chatGifInfo?.let { giphy ->
            GiphyMessageView(
                gifInfo = giphy,
                title = giphy.title,
                autoPlayGif = if (autoPlay) true else giphy.webpSize < MAX_SIZE_FOR_AUTO_PLAY,
                onLoaded = { autoPlay = true },
                onLongClick = { onLongClick(message) }
            )
        }
    }

    override val showAvatar = message.shouldShowAvatar
    override val showTime = message.shouldShowTime
    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = true
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
    private var autoPlay: Boolean = false
}
