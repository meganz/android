package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.PendingAttachmentMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.PendingAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Pending attachment ui message
 */
data class PendingAttachmentUiMessage(
    override val message: PendingAttachmentMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {

    @Composable
    override fun RowScope.ContentComposable(onLongClick: (TypedMessage) -> Unit) {
        PendingAttachmentMessageView(message)
    }

    override val showTime = message.shouldShowTime
    override val showAvatar = message.shouldShowAvatar
    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = false
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId


}