package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    abstract fun ContentComposable(
        onLongClick: (TypedMessage) -> Unit,
        interactionEnabled: Boolean,
    )

    abstract override val message: TypedMessage
    
    /**
     * Avatar composable
     */
    @Composable
    open fun MessageAvatar(lastUpdatedCache: Long, modifier: Modifier) {
        if (showAvatar) {
            ChatAvatar(
                modifier = modifier,
                handle = userHandle,
                lastUpdatedCache = lastUpdatedCache
            )
        } else {
            Spacer(modifier = modifier)
        }
    }

    /**
     * Show avatar
     */
    abstract val showAvatar: Boolean

    @Composable
    override fun MessageListItem(
        state: UIMessageState,
        onLongClick: (TypedMessage) -> Unit,
        onMoreReactionsClicked: (Long) -> Unit,
        onReactionClicked: (Long, String, List<UIReaction>) -> Unit,
        onReactionLongClick: (String, List<UIReaction>) -> Unit,
        onForwardClicked: (TypedMessage) -> Unit,
        onSelectedChanged: (Boolean) -> Unit,
        onSendErrorClicked: (TypedMessage) -> Unit,
    ) {
        ChatMessageContainer(
            modifier = Modifier.fillMaxWidth(),
            isMine = displayAsMine,
            showForwardIcon = shouldDisplayForwardIcon,
            reactions = reactions,
            onMoreReactionsClick = { onMoreReactionsClicked(id) },
            onReactionClick = { onReactionClicked(id, it, reactions) },
            onReactionLongClick = { onReactionLongClick(it, reactions) },
            onForwardClicked = { onForwardClicked(message) },
            avatarOrIcon = { avatarModifier ->
                MessageAvatar(
                    lastUpdatedCache = state.lastUpdatedCache,
                    avatarModifier,
                )
            },
            content = { interactionEnabled ->
                ContentComposable(onLongClick, interactionEnabled)
            },
            isSelectMode = state.isInSelectMode,
            isSelected = state.isChecked,
            onSelectionChanged = onSelectedChanged,
            onSendErrorClick = { onSendErrorClicked(message) }
        )
    }

    override val isSelectable = true

    override fun key() = super.key() + "_${showAvatar}"
}