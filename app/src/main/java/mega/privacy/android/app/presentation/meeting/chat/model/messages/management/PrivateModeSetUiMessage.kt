package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.PrivateModeSetMessageView
import mega.privacy.android.shared.original.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.management.PrivateModeSetMessage


/**
 * Private mode set ui message
 *
 */
data class PrivateModeSetUiMessage(
    override val message: PrivateModeSetMessage,
    override val reactions: List<UIReaction>,
) : ManagementUiChatMessage() {

    override val contentComposable: @Composable () -> Unit = {
        PrivateModeSetMessageView(
            message = message,
        )
    }
}
