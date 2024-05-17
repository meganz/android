package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.TitleChangeMessageView
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.management.TitleChangeMessage

/**
 * Title change ui message
 *
 * @property message
 */
data class TitleChangeUiMessage(
    override val message: TitleChangeMessage,
    override val reactions: List<UIReaction>,
) : ManagementUiChatMessage() {
    override val contentComposable: @Composable () -> Unit = {
        TitleChangeMessageView(message = message)
    }
}