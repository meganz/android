package mega.privacy.android.app.presentation.meeting.managechathistory.model

/**
 * UI state for manage chat history screen.
 *
 * @property retentionTimeUpdate The updated retention time.
 * @property shouldShowClearChatConfirmation True if should show the clear chat confirmation, false otherwise
 * @property shouldNavigateUp True if we should navigate to the previous screen, false otherwise
 */
data class ManageChatHistoryUIState(
    val retentionTimeUpdate: Long? = null,
    val shouldShowClearChatConfirmation: Boolean = false,
    val shouldNavigateUp: Boolean = false,
)
