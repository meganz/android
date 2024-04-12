package mega.privacy.android.app.presentation.meeting.managechathistory.model

import mega.privacy.android.app.presentation.chat.model.ChatRoomUiState

/**
 * UI state for manage chat history screen.
 *
 * @property chatRoom The current [ChatRoomUiState].
 * @property retentionTime The chat history retention time.
 * @property shouldNavigateUp True if we should navigate to the previous screen, false otherwise
 */
data class ManageChatHistoryUIState(
    val chatRoom: ChatRoomUiState? = null,
    val retentionTime: Long = 0L,
    val shouldNavigateUp: Boolean = false,
)
