package mega.privacy.android.app.presentation.meeting.managechathistory.model

/**
 * UI state for manage chat history screen.
 *
 * @property retentionTimeUpdate The updated retention time.
 * @property shouldShowClearChatConfirmation True if should show the clear chat confirmation, false otherwise
 */
data class ManageChatHistoryUIState(
    val retentionTimeUpdate: Long? = null,
    val shouldShowClearChatConfirmation: Boolean = false,
)
