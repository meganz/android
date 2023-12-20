package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

/**
 * UI chat message
 *
 */
interface UiChatMessage {
    /**
     * Content composable
     */
    val contentComposable: @Composable RowScope.() -> Unit

    /**
     * Avatar composable
     */
    val avatarComposable: (@Composable RowScope.() -> Unit)?

    /**
     * Modifier
     */
    val modifier: Modifier
        get() = Modifier.fillMaxWidth()

    /**
     * Message
     */
    val message: TypedMessage

    /**
     * Show avatar
     */
    val showAvatar: Boolean

    /**
     * Show time
     */
    val showTime: Boolean

    /**
     * Show date
     */
    val showDate: Boolean
}