package mega.privacy.android.app.presentation.meeting.chat.model.messages.normal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.extension.canForward
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.voiceclip.VoiceClipMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.VoiceClipMessage

/**
 * UI message for voice clip
 *
 * @property message [VoiceClipMessage]
 * @property chatId
 */
class VoiceClipUiMessage(
    val message: VoiceClipMessage,
    val chatId: Long,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun RowScope.ContentComposable(onLongClick: (TypedMessage) -> Unit) {
        VoiceClipMessageView(
            message = message,
            chatId = chatId,
            modifier = Modifier.combinedClickable(
                onClick = {},
                onLongClick = { onLongClick(message) }
            )
        )
    }

    override val showAvatar = message.shouldShowAvatar
    override val showTime = message.shouldShowTime
    override val showDate = message.shouldShowDate
    override val displayAsMine = message.isMine
    override val canForward = message.canForward
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}