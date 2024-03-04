package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.RetentionTimeUpdatedMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.management.RetentionTimeUpdatedMessage

/**
 * Retention time updated ui message
 *
 * @property message
 */
data class RetentionTimeUpdatedUiMessage(
    override val message: RetentionTimeUpdatedMessage,
    override val reactions: List<UIReaction>,
) : ManagementUiChatMessage() {
    override val contentComposable: @Composable () -> Unit = {
        RetentionTimeUpdatedMessageView(
            message = message,
        )
    }
}