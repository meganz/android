package mega.privacy.android.app.presentation.meeting.chat.model.messages.normal

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.extension.canForward
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.core.ui.controls.chat.messages.VoiceClipMessageView
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage

class VoiceClipUiMessage(
    val message: VoiceClipMessage,
    override val showAvatar: Boolean,
    override val showTime: Boolean,
    override val showDate: Boolean,
) : AvatarMessage() {

    override val contentComposable: @Composable (RowScope.() -> Unit) = {
        VoiceClipMessageView(
            isMe = true,
            timestamp = "${message.duration}",
            loadProgress = null,
            playProgress = null,
            isPlaying = false,
            onPlayClicked = {},
        )
    }

    override val displayAsMine = message.isMine
    override val canForward = message.canForward
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}