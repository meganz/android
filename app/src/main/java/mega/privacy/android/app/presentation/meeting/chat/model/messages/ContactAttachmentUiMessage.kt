package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.meeting.chat.view.message.contact.ContactAttachmentMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.ContactAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Contact attachment ui message
 *
 * @property message
 * @property showAvatar
 * @property showTime
 */
data class ContactAttachmentUiMessage(
    override val message: ContactAttachmentMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {

    @Composable
    override fun ContentComposable(
        onLongClick: (TypedMessage) -> Unit,
        interactionEnabled: Boolean
    ) {
        ContactAttachmentMessageView(
            message = message,
            onLongClick = { onLongClick(message) },
            modifier = Modifier,
            interactionEnabled = interactionEnabled,
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