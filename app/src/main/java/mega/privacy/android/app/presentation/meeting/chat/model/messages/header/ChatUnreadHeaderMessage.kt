package mega.privacy.android.app.presentation.meeting.chat.model.messages.header

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UIMessageState
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.controls.chat.ChatUnreadMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Chat header message
 */
class ChatUnreadHeaderMessage(private val unreadCount: Int) : HeaderMessage() {

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
        ChatUnreadMessageView(
            content = pluralStringResource(
                id = R.plurals.number_unread_messages,
                count = unreadCount,
                "$unreadCount"
            )
        )
    }

    override fun key(): String = "chat_unread_message_header"
}