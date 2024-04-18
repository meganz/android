package mega.privacy.android.app.presentation.meeting.chat.model.messages.meta

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.meta.GiphyMessageView
import mega.privacy.android.app.presentation.meeting.chat.view.navigation.openGiphyViewerActivity
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.meta.GiphyMessage

private const val MAX_SIZE_FOR_AUTO_PLAY = 1024 * 1024 * 4  // 4MB

/**
 * Ui message for Giphy message
 *
 * @property message
 * @property showAvatar
 */
class ChatGiphyUiMessage(
    override val message: GiphyMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {

    @Composable
    override fun ContentComposable(
        interactionEnabled: Boolean,
        onLongClick: () -> Unit,
        initialiseModifier: (onClick: () -> Unit) -> Modifier,
    ) {
        message.chatGifInfo?.let { giphy ->
            var autoPlayGif: Boolean by remember { mutableStateOf(giphy.webpSize < MAX_SIZE_FOR_AUTO_PLAY) }
            val context = LocalContext.current

            val onClick = {
                if (!autoPlayGif) {
                    autoPlayGif = true
                } else {
                    openGiphyViewerActivity(context, giphy)
                }
            }

            GiphyMessageView(
                gifInfo = giphy,
                autoPlay = autoPlayGif,
                modifier = initialiseModifier(onClick),
                title = giphy.title,
                onLoaded = { autoPlayGif = true },
                onError = { autoPlayGif = false }
            )
        }
    }

    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = true
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}
