package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UIMessageState
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.core.ui.controls.chat.ChatMessageContainer
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.management.ManagementMessage

/**
 * Management ui chat message
 */
abstract class ManagementUiChatMessage : UiChatMessage {
    /**
     * Message
     */
    abstract override val message: ManagementMessage

    override val showTime: Boolean = true

    override val displayAsMine = false

    override val isSelectable: Boolean = false

    /**
     * Content composable
     */
    abstract val contentComposable: @Composable (RowScope.() -> Unit)

    @Composable
    override fun MessageListItem(
        state: UIMessageState,
        timeFormatter: (Long) -> String,
        dateFormatter: (Long) -> String,
        onLongClick: (TypedMessage) -> Unit,
        onMoreReactionsClicked: (Long) -> Unit,
        onReactionClicked: (Long, String, List<UIReaction>) -> Unit,
        onReactionLongClick: (String, List<UIReaction>) -> Unit,
        onForwardClicked: (TypedMessage) -> Unit,
        onSelectedChanged: (Boolean) -> Unit,
    ) {
        ChatMessageContainer(
            modifier = Modifier.fillMaxWidth(),
            isMine = displayAsMine,
            showForwardIcon = shouldDisplayForwardIcon,
            reactions = reactions,
            onMoreReactionsClick = { onMoreReactionsClicked(id) },
            onReactionClick = { onReactionClicked(id, it, reactions) },
            onForwardClicked = { onForwardClicked(message) },
            onReactionLongClick = { onReactionLongClick(it, reactions) },
            time = getTimeOrNull(timeFormatter),
            content = { _ -> contentComposable() },
            onSelectionChanged = onSelectedChanged,
        )
    }

    override val shouldDisplayForwardIcon = false

    override val timeSent: Long
        get() = message.time

    override val userHandle: Long
        get() = message.userHandle

    override val id: Long
        get() = message.msgId
}