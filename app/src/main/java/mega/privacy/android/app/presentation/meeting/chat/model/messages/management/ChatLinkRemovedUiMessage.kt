package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.ChatLinkRemovedView
import mega.privacy.android.core.ui.controls.chat.messages.reaction.model.UIReaction
import mega.privacy.android.domain.entity.chat.messages.management.ChatLinkRemovedMessage


/**
 * Chat link removed UI message.
 */
data class ChatLinkRemovedUiMessage(
    override val message: ChatLinkRemovedMessage,
    override val reactions: List<UIReaction>,
) : ManagementUiChatMessage() {

    override val contentComposable: @Composable () -> Unit = {
        ChatLinkRemovedView(
            message = message,
        )
    }
}
