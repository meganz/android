package mega.privacy.android.app.presentation.meeting.chat.mapper

import androidx.compose.foundation.layout.RowScope
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
    operator fun invoke(
        message: TypedMessage,
        isOneToOne: Boolean,
        showAvatar: Boolean,
    ): UiChatMessage {
        return when (message) {
            is TextMessage -> TextUiMessage(message = message, showAvatar = showAvatar)
            is CallMessage -> CallUiMessage(message = message, isOneToOneChat = isOneToOne)
            // will remove when add new message type
            else -> object : UiChatMessage {
                override val contentComposable: @Composable (RowScope.() -> Unit) = {

                }
                override val avatarComposable: @Composable (RowScope.() -> Unit)? = null
                override val message: TypedMessage = message

                override val showAvatar: Boolean = false
            }
        }
    }
}