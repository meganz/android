package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.core.ui.controls.chat.ChatMessageContainer

/**
 * Avatar message
 */
abstract class AvatarMessage : UiChatMessage {
    /**
     * Content composable
     */
    abstract val contentComposable: @Composable() (RowScope.() -> Unit)

    /**
     * Avatar composable
     */
    abstract val avatarComposable: @Composable() (RowScope.() -> Unit)

    /**
     * Show avatar
     */
    abstract val showAvatar: Boolean

    @Composable
    override fun MessageListItem(
        uiState: ChatUiState,
        timeFormatter: (Long) -> String,
        dateFormatter: (Long) -> String,
    ) {
        ChatMessageContainer(
            modifier = Modifier.fillMaxWidth(),
            isMine = displayAsMine,
            showForwardIcon = canForward,
            time = this.getTimeOrNull(timeFormatter),
            date = this.getDateOrNull(dateFormatter),
            avatarOrIcon = avatarComposable,
            content = contentComposable,
        )
    }

    override fun key(): String {
        return super.key() + "_${showAvatar}"
    }
}