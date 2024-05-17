package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ChatTruncateHistoryMessageView
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.management.TruncateHistoryMessage


/**
 * Truncate history UI message.
 */
data class TruncateHistoryUiMessage(
    override val message: TruncateHistoryMessage,
    override val reactions: List<UIReaction>,
) : ManagementUiChatMessage() {

    override val contentComposable: @Composable () -> Unit = {
        ChatTruncateHistoryMessageView(
            message = message,
        )
    }
}
