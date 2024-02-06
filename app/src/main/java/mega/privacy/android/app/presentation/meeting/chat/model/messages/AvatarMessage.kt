package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.view.ChatAvatar
import mega.privacy.android.core.ui.controls.chat.ChatMessageContainer
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Avatar message
 */
abstract class AvatarMessage : UiChatMessage {
    /**
     * Content composable
     */
    @Composable
    abstract fun RowScope.ContentComposable(onLongClick: (TypedMessage) -> Unit)

    /**
     * Avatar composable
     */
    @Composable
    open fun RowScope.MessageAvatar(lastUpdatedCache: Long) {
        if (showAvatar) {
            ChatAvatar(
                modifier = Modifier.align(Alignment.Bottom),
                handle = userHandle,
                lastUpdatedCache = lastUpdatedCache
            )
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
    }

    /**
     * Show avatar
     */
    abstract val showAvatar: Boolean

    @Composable
    override fun MessageListItem(
        uiState: ChatUiState,
        lastUpdatedCache: Long,
        timeFormatter: (Long) -> String,
        dateFormatter: (Long) -> String,
        onLongClick: (TypedMessage) -> Unit,
        onMoreReactionsClicked: (Long) -> Unit,
        onReactionClicked: (Long, String, List<UIReaction>) -> Unit,
    ) {
        ChatMessageContainer(
            modifier = Modifier.fillMaxWidth(),
            isMine = displayAsMine,
            showForwardIcon = canForward,
            reactions = reactions,
            onMoreReactionsClicked = { onMoreReactionsClicked(id) },
            onReactionClicked = { onReactionClicked(id, it, reactions) },
            time = this.getTimeOrNull(timeFormatter),
            date = this.getDateOrNull(dateFormatter),
            avatarOrIcon = {
                MessageAvatar(
                    lastUpdatedCache = lastUpdatedCache
                )
            },
            content = {
                ContentComposable(onLongClick)
            },
        )
    }

    override fun key(): String {
        return super.key() + "_${showAvatar}"
    }
}