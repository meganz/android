package mega.privacy.android.app.presentation.meeting.chat.model.messages.header

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.pluralStringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import mega.privacy.android.app.presentation.meeting.chat.model.MessageListViewModel
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UIMessageState
import mega.privacy.android.core.R
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.shared.original.core.ui.controls.chat.ChatUnreadMessageView
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction

/**
 * Chat header message
 */
class ChatUnreadHeaderMessage(
    private val unreadCount: Int,
    override val message: TypedMessage?,
) : HeaderMessage() {

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
        navHostController: NavHostController,
    ) {
        val viewModel = hiltViewModel<MessageListViewModel>()
        val listState by viewModel.state.collectAsStateWithLifecycle()
        val finalUnreadCount = unreadCount + listState.extraUnreadCount
        ChatUnreadMessageView(
            content = pluralStringResource(
                id = R.plurals.number_unread_messages,
                count = finalUnreadCount,
                "$finalUnreadCount"
            )
        )
    }

    override val timeSent: Long? = message?.time

    override fun key(): String = "chat_unread_message_header"
}