package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ChatCallMessageView
import mega.privacy.android.core.ui.controls.chat.ChatMessageContainer
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.management.CallMessage

/**
 * Call ui chat message
 *
 * @property message
 */
data class CallUiMessage(
    override val message: CallMessage,
    override val reactions: List<UIReaction>,
) : UiChatMessage {

    override val displayAsMine = false

    @Composable
    override fun MessageListItem(
        state: UIMessageState,
        onLongClick: (TypedMessage) -> Unit,
        onMoreReactionsClicked: (Long) -> Unit,
        onReactionClicked: (Long, String, List<UIReaction>) -> Unit,
        onReactionLongClick: (String, List<UIReaction>) -> Unit,
        onForwardClicked: (TypedMessage) -> Unit,
        onSelectedChanged: (Boolean) -> Unit,
        onNotSentClick: (TypedMessage) -> Unit,
    ) {
        ChatMessageContainer(
            isMine = displayAsMine,
            showForwardIcon = shouldDisplayForwardIcon,
            reactions = reactions,
            onMoreReactionsClick = { onMoreReactionsClicked(id) },
            onReactionClick = { onReactionClicked(id, it, reactions) },
            onReactionLongClick = { onReactionLongClick(it, reactions) },
            onForwardClicked = { onForwardClicked(message) },
            modifier = Modifier.fillMaxWidth(),
            onSelectionChanged = onSelectedChanged,
        ) {
            ChatCallMessageView(
                message = message,
                isOneToOneChat = state.isOneToOne
            )
        }
    }

    override val shouldDisplayForwardIcon = false
    override val timeSent = message.time
    override val userHandle = message.userHandle
    override val id = message.msgId
    override val isSelectable = false
}