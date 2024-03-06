package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.PendingAttachmentMessageView
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.PendingVoiceClipMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.PendingAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingVoiceClipMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Pending attachment ui message
 */
data class PendingAttachmentUiMessage(
    override val message: PendingAttachmentMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {

    @Composable
    override fun ContentComposable(
        onLongClick: (TypedMessage) -> Unit,
        interactionEnabled: Boolean,
    ) {
        when (message) {
            is PendingVoiceClipMessage -> PendingVoiceClipMessageView(message)
            is PendingFileAttachmentMessage -> PendingAttachmentMessageView(message)
        }
    }

    override val showAvatar = message.shouldShowAvatar
    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = false
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId


}