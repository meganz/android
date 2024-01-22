package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.extension.canForward
import mega.privacy.android.app.presentation.meeting.chat.extension.canLongClick
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.presentation.meeting.chat.model.messages.UiChatMessage
import mega.privacy.android.core.ui.controls.chat.ChatMessageContainer
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.management.ManagementMessage

/**
 * Management ui chat message
 */
abstract class ManagementUiChatMessage : UiChatMessage {
    /**
     * Message
     */
    abstract val message: ManagementMessage

    override val showTime: Boolean = true

    override val displayAsMine = false

    /**
     * Content composable
     */
    abstract val contentComposable: @Composable() (RowScope.() -> Unit)

    @Composable
    override fun MessageListItem(
        uiState: ChatUiState,
        lastUpdatedCache: Long,
        timeFormatter: (Long) -> String,
        dateFormatter: (Long) -> String,
        onLongClick: (TypedMessage) -> Unit,
    ) {
        ChatMessageContainer(
            modifier = Modifier.fillMaxWidth(),
            isMine = displayAsMine,
            showForwardIcon = canForward,
            time = getTimeOrNull(timeFormatter),
            date = getDateOrNull(dateFormatter),
            content = contentComposable,
        )
    }

    override val canForward: Boolean
        get() = message.canForward

    override val timeSent: Long
        get() = message.time

    override val userHandle: Long
        get() = message.userHandle

    override val canLongClick: Boolean
        get() = message.canLongClick

    override val id: Long
        get() = message.msgId
}