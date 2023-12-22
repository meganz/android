package mega.privacy.android.app.presentation.meeting.chat.model.messages

import android.content.Context
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.app.utils.TimeUtils
import mega.privacy.android.core.ui.controls.chat.ChatMessageContainer

/**
 * UI chat message
 *
 */
interface UiChatMessage {

    /**
     * Id
     */
    val id: Long

    /**
     * Content composable
     */
    val contentComposable: @Composable RowScope.() -> Unit

    /**
     * Avatar composable
     */
    val avatarComposable: (@Composable RowScope.() -> Unit)?

    /**
     * Render message ui
     *
     * @param uiState
     * @param context
     */
    @Composable
    fun MessageListItem(uiState: ChatUiState, context: Context) {
        ChatMessageContainer(
            modifier = Modifier.fillMaxWidth(),
            isMine = displayAsMine,
            showForwardIcon = canForward,
            time = getTimeOrNull(this),
            date = getDateOrNull(this, context),
            avatarOrIcon = avatarComposable,
            content = contentComposable,
        )
    }

    private fun getTimeOrNull(uiChatMessage: UiChatMessage) =
        if (uiChatMessage.showTime) uiChatMessage.timeSent?.let {
            TimeUtils.formatTime(
                it,
            )
        } else null

    private fun getDateOrNull(
        uiChatMessage: UiChatMessage,
        context: Context,
    ) = if (uiChatMessage.showDate) uiChatMessage.timeSent?.let {
        TimeUtils.formatDate(
            it,
            TimeUtils.DATE_SHORT_FORMAT,
            context,
        )
    } else null

    /**
     * Modifier
     */
    val modifier: Modifier
        get() = Modifier.fillMaxWidth()

    /**
     * Display as mine
     */
    val displayAsMine: Boolean

    /**
     * Can forward
     */
    val canForward: Boolean

    /**
     * Time sent
     */
    val timeSent: Long?

    /**
     * User handle
     */
    val userHandle: Long?

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