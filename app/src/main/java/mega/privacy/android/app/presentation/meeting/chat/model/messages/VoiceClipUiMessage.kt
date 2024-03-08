package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.view.message.voiceclip.VoiceClipMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage

/**
 * UI message for voice clip
 *
 * @property message [VoiceClipMessage]
 */
class VoiceClipUiMessage(
    override val message: VoiceClipMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {

    @Composable
    override fun ContentComposable(
        onLongClick: (TypedMessage) -> Unit,
        interactionEnabled: Boolean,
    ) {
        VoiceClipMessageView(
            message = message,
            onLongClick = onLongClick,
            interactionEnabled = interactionEnabled,
        )
    }

    override val showAvatar = message.shouldShowAvatar
    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = message.exists
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}