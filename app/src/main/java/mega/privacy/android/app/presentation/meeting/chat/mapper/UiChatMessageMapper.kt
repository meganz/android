package mega.privacy.android.app.presentation.meeting.chat.mapper

import androidx.compose.runtime.Composable
import mega.privacy.android.app.presentation.meeting.chat.model.ui.CallUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.TextUiMessage
import mega.privacy.android.app.presentation.meeting.chat.model.ui.UiChatMessage
import mega.privacy.android.domain.entity.chat.messages.TypedMessage
import mega.privacy.android.domain.entity.chat.messages.management.CallMessage
import mega.privacy.android.domain.entity.chat.messages.normal.TextMessage
import javax.inject.Inject

/**
 * Mapper to convert a [TypedMessage] to a [UiChatMessage]
 *
 */
class UiChatMessageMapper @Inject constructor() {
    /**
     * Invoke
     *
     * @param message
     * @param isOneToOne
     */
    operator fun invoke(message: TypedMessage, isOneToOne: Boolean): UiChatMessage {
        return when (message) {
            is TextMessage -> TextUiMessage(message)
            is CallMessage -> CallUiMessage(message, isOneToOne)
            // will remove when add new message type
            else -> object : UiChatMessage {
                override val contentComposable: @Composable () -> Unit = {

                }
                override val message: TypedMessage = message
            }
        }
    }
}