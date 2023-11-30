package mega.privacy.android.app.presentation.meeting.chat.model.ui

import androidx.compose.runtime.Composable
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * UI chat message
 *
 */
interface UiChatMessage {
    /**
     * Content composable
     */
    val contentComposable: @Composable () -> Unit

    /**
     * Message
     */
    val message: TypedMessage
}