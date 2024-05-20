package mega.privacy.android.app.presentation.meeting.chat.model.messages.header

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UIMessageState
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.DateHeader
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * Date header ui message
 *
 * @property timeSent
 */
class DateHeaderUiMessage(
    override val timeSent: Long,
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
        val context = LocalContext.current
        DateHeader(
            TimeUtils.formatDate(
                timeSent,
                TimeUtils.DATE_SHORT_FORMAT,
                context
            )
        )
    }

    override fun key() = timeSent.toString()

}