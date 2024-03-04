package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.AlterParticipantsMessageView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.management.AlterParticipantsMessage

/**
 * Alter participants ui message
 *
 * @property message
 */
data class AlterParticipantsUiMessage(
    override val message: AlterParticipantsMessage,
    override val reactions: List<UIReaction>,
) : ParticipantUiMessage() {
    override val contentComposable: @Composable () -> Unit = {
        AlterParticipantsMessageView(message = message)
    }

    override val handleOfAction: Long
        get() = message.handleOfAction
}