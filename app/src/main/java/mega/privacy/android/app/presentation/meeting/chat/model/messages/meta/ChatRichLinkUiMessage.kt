package mega.privacy.android.app.presentation.meeting.chat.model.messages.meta

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.meta.ChatRichLinkMessageView
import mega.privacy.android.core.ui.controls.chat.messages.OpenMessageLink
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.meta.RichPreviewMessage

/**
 * Chat rich link ui message
 *
 * @property message
 * @property showAvatar
 */
data class ChatRichLinkUiMessage(
    override val message: RichPreviewMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {
    @Composable
    override fun ContentComposable(
        interactionEnabled: Boolean,
        onLongClick: () -> Unit,
        initialiseModifier: (onClick: () -> Unit) -> Modifier,
    ) {
        var clickedLink by rememberSaveable { mutableStateOf<String?>(null) }

        clickedLink?.let {
            OpenMessageLink(link = it)
            clickedLink = null
        }

        ChatRichLinkMessageView(
            message = message,
            interactionEnabled = interactionEnabled,
            onLongClick = onLongClick,
            modifier = initialiseModifier {
                clickedLink = message.chatRichPreviewInfo?.url
            },
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
    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = true
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}