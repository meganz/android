package mega.privacy.android.app.presentation.meeting.chat.model.messages.management

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.AlterParticipantsMessageView
import mega.privacy.android.domain.entity.chat.messages.management.AlterParticipantsMessage

/**
 * Alter participants ui message
 *
 * @property message
 * @property showDate
 */
data class AlterParticipantsUiMessage(
    override val message: AlterParticipantsMessage,
    override val showDate: Boolean,
) : ManagementUiChatMessage() {
    override val contentComposable: @Composable (RowScope.() -> Unit) = {
        AlterParticipantsMessageView(message = message, modifier = Modifier.padding(start = 32.dp))
    }
}