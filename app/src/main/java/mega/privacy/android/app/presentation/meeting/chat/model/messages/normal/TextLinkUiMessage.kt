package mega.privacy.android.app.presentation.meeting.chat.model.messages.normal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.link.ChatLinksMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextLinkMessage

/**
 * Contact link ui message
 * @property message Contact link message
 *
 */
data class TextLinkUiMessage(
    override val message: TextLinkMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {
    @Composable
    override fun ContentComposable(
        onLongClick: (TypedMessage) -> Unit,
        interactionEnabled: Boolean,
    ) {
        ChatLinksMessageView(
            modifier = Modifier,
            onLongClick = onLongClick,
            message = message,
            interactionEnabled = interactionEnabled,
        )
    }

    override val showAvatar = message.shouldShowAvatar
    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = true
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}