package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.view.ChatAvatar
import mega.privacy.android.app.presentation.meeting.chat.view.LastItemAvatarPosition
import mega.privacy.android.core.ui.controls.chat.ChatMessageContainer
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.core.ui.theme.extensions.conditional
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Avatar message
 */
@OptIn(ExperimentalFoundationApi::class)
abstract class AvatarMessage : UiChatMessage {

    /**
     * Content composable
     *
     * @param onLongClick Already established in the Modifier, but required by some type of messages
     * with text in bubbles and others which has interactions with their content.
     */
    @Composable
    abstract fun ContentComposable(
        interactionEnabled: Boolean,
        onLongClick: () -> Unit,
        initialiseModifier: (onClick: () -> Unit) -> Modifier,
    )

    abstract override val message: TypedMessage

    /**
     * Avatar composable
     */
    @Composable
    open fun MessageAvatar(
        lastUpdatedCache: Long,
        modifier: Modifier,
        lastItemAvatarPosition: LastItemAvatarPosition?,
    ) {
        if (showAvatar(lastItemAvatarPosition)) {
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
    private fun showAvatar(lastItemAvatarPosition: LastItemAvatarPosition?): Boolean =
        lastItemAvatarPosition?.shouldBeDrawnByMessage() == true


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
            isSelectMode = state.isInSelectMode,
            isSelected = state.isChecked,
            onSelectionChanged = onSelectedChanged,
            avatarOrIcon = { avatarModifier ->
                MessageAvatar(
                    lastUpdatedCache = state.lastUpdatedCache,
                    avatarModifier,
                    state.lastItemAvatarPosition,
                )
            },
            avatarAlignment = if (state.lastItemAvatarPosition == LastItemAvatarPosition.Top) {
                Alignment.Top
            } else {
                Alignment.Bottom
            },
            isSendError = message.isSendError()
        ) { interactionEnabled ->
            ContentComposable(
                interactionEnabled = interactionEnabled,
                onLongClick = {
                    if (interactionEnabled) {
                        onLongClick(message)
                    }
                },
                initialiseModifier = {
                    Modifier.contentInteraction(
                        onNotSentClick = onNotSentClick,
                        onClick = it,
                        onLongClick = onLongClick,
                        interactionEnabled = interactionEnabled
                    )
                }
            )
        }
    }

    private fun Modifier.contentInteraction(
        onNotSentClick: (TypedMessage) -> Unit,
        onClick: () -> Unit,
        onLongClick: (TypedMessage) -> Unit,
        interactionEnabled: Boolean,
    ) = if (message.isNotSent()) {
        forNotSent(
            onNotSentClick = { onNotSentClick(message) }
        )
    } else {
        setClickHandlers(
            onClick = onClick,
            onLongClick = { onLongClick(message) },
            interactionEnabled = interactionEnabled,
        )
    }

    private fun Modifier.forNotSent(onNotSentClick: () -> Unit) = this.combinedClickable(
        onClick = onNotSentClick,
        onLongClick = onNotSentClick,
    )

    private fun Modifier.setClickHandlers(
        onClick: () -> Unit,
        onLongClick: () -> Unit,
        interactionEnabled: Boolean,
    ) = this.conditional(interactionEnabled) {
        combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    }

    override val isSelectable = true

    override fun key() = super.key()
}