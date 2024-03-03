package mega.privacy.android.app.presentation.meeting.chat.model.messages.normal

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.model.messages.AvatarMessage
import mega.privacy.android.app.presentation.meeting.chat.view.message.normal.ChatMessageTextView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.core.ui.theme.extensions.conditional
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage

/**
 * Text u i chat message
 *
 * @property message Text message
 */
data class TextUiMessage(
    override val message: TextMessage,
    override val reactions: List<UIReaction>,
) : AvatarMessage() {
    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    override fun ContentComposable(
        onLongClick: (TypedMessage) -> Unit,
        interactionEnabled: Boolean,
    ) {
        ChatMessageTextView(
            message = message,
            isEdited = message.isEdited,
            modifier = Modifier.conditional(interactionEnabled) {
                combinedClickable(
                    onClick = {},
                    onLongClick = { onLongClick(message) }
                )
            },
        )
    }


    override val showAvatar = message.shouldShowAvatar
    override val displayAsMine = message.isMine
    override val shouldDisplayForwardIcon = false
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
}