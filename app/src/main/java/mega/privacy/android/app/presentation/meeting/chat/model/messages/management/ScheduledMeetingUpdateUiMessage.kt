package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ScheduledMeetingUpdateMessageView
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.management.ScheduledMeetingUpdatedMessage

/**
 * Scheduled meeting update ui message
 *
 * @property message
 */
data class ScheduledMeetingUpdateUiMessage(
    override val message: ScheduledMeetingUpdatedMessage,
    override val reactions: List<UIReaction>,
) : ManagementUiChatMessage() {
    override val contentComposable: @Composable () -> Unit = {
        ScheduledMeetingUpdateMessageView(
            message = message,
        )
    }
}