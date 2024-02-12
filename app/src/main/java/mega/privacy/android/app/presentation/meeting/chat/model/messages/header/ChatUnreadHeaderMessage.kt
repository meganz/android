package mega.privacy.android.app.presentation.meeting.chat.model.messages.header

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.chat.ChatUnreadMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Chat header message
 */
class ChatUnreadHeaderMessage(private val unreadCount: Int) : UiChatMessage {

    @Composable
    override fun MessageListItem(
        uiState: ChatUiState,
        lastUpdatedCache: Long,
        timeFormatter: (Long) -> String,
        dateFormatter: (Long) -> String,
        onLongClick: (TypedMessage) -> Unit,
        onMoreReactionsClicked: (Long) -> Unit,
        onReactionClicked: (Long, String, List<UIReaction>) -> Unit,
        onReactionLongClick: (String, List<UIReaction>) -> Unit,
        onForwardClicked: (TypedMessage) -> Unit,
    ) {
        ChatUnreadMessageView(
            content = pluralStringResource(
                id = R.plurals.number_unread_messages,
                count = unreadCount,
                "$unreadCount"
            )
        )
    }

    override val id = -1L
    override val displayAsMine = false
    override val shouldDisplayForwardIcon = false
    override val timeSent = null
    override val userHandle = -1L
    override val showTime = false
    override val reactions = emptyList<UIReaction>()
    override val isSelectable = false

    override fun key(): String = "chat_unread_message_header"
}