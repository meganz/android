package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.view.message.voiceclip.VoiceClipMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.core.ui.theme.extensions.conditional
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

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun ContentComposable(
        onLongClick: (TypedMessage) -> Unit,
        interactionEnabled: Boolean,
    ) {
        VoiceClipMessageView(
            message = message,
            chatId = message.chatId,
            modifier = Modifier
                .conditional(interactionEnabled) {
                    combinedClickable(
                        onClick = {},
                        onLongClick = { onLongClick(message) }
                    )
                },
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