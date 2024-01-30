package mega.privacy.android.app.presentation.meeting.chat.model.messages

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.privacy.android.app.presentation.meeting.chat.model.ChatUiState
import mega.privacy.android.domain.entity.chat.messages.TypedMessage

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
     * Message list item
     *
     * @param uiState
     * @param timeFormatter
     * @param dateFormatter
     */
    @Composable
    fun MessageListItem(
        uiState: ChatUiState,
        lastUpdatedCache: Long,
        timeFormatter: (Long) -> String,
        dateFormatter: (Long) -> String,
        onLongClick: (TypedMessage) -> Unit,
    )

    /**
     * Get time or null
     *
     * @param timeFormatter
     */
    fun getTimeOrNull(timeFormatter: (Long) -> String) =
        if (showTime) timeSent?.let {
            timeFormatter(it)
        } else null

    /**
     * Get date or null
     *
     * @param dateFormatter
     */
    fun getDateOrNull(
        dateFormatter: (Long) -> String,
    ) = if (showDate) timeSent?.let {
        dateFormatter(it)
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
    val userHandle: Long

    /**
     * Show time
     */
    val showTime: Boolean

    /**
     * Show date
     */
    val showDate: Boolean

    /**
     * Key
     */
    fun key(): String = "${id}_${showTime}_${showDate}"
}