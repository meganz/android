package mega.privacy.android.app.presentation.meeting.chat.model.messages.header

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.core.ui.controls.chat.messages.DateHeader
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Date header ui message
 *
 * @property timeSent
 */
class DateHeaderUiMessage(override val timeSent: Long) : HeaderMessage() {

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
        DateHeader(dateFormatter(timeSent))
    }

    override fun key() = timeSent.toString()

}