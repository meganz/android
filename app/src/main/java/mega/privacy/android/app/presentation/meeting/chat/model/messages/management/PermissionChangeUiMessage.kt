package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.permission.PermissionChangeMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.management.PermissionChangeMessage

/**
 * Permission change ui message
 *
 * @property message
 */
data class PermissionChangeUiMessage(
    override val message: PermissionChangeMessage,
    override val reactions: List<UIReaction>,
) : ParticipantUiMessage() {
    override val contentComposable: @Composable () -> Unit = {
        PermissionChangeMessageView(message = message)
    }

    override val handleOfAction: Long
        get() = message.handleOfAction
}