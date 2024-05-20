package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.PendingAttachmentMessageView
import mega.privacy.android.app.presentation.meeting.chat.view.message.attachment.PendingVoiceClipMessageView
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.PendingAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingFileAttachmentMessage
import mega.privacy.android.domain.entity.chat.messages.PendingVoiceClipMessage

/**
 * Pending attachment ui message
 */
data class PendingAttachmentUiMessage(
    override val message: PendingAttachmentMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {

    @Composable
    override fun ContentComposable(
        interactionEnabled: Boolean,
        onLongClick: () -> Unit,
        initialiseModifier: (onClick: () -> Unit) -> Modifier,
        navHostController: NavHostController,
    ) {
        val modifier = initialiseModifier {
            //click here will never occur as a pending message is always a not sent message
        }
        when (message) {
            is PendingVoiceClipMessage -> PendingVoiceClipMessageView(message, modifier)
            is PendingFileAttachmentMessage -> PendingAttachmentMessageView(message, modifier)
        }
    }

    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = false
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}