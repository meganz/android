package mega.privacy.android.app.presentation.meeting.managechathistory.model

import androidx.annotation.StringRes
import mega.privacy.android.app.R

/**
 * UI state for manage chat history screen.
 *
 * @property selectedHistoryRetentionTimeOption The current selected chat history retention time.
 * @property confirmButtonStringId The string resource ID for the confirm button text
 * @property isConfirmButtonEnable True if should enable the confirm button of the history retention confirmation, false otherwise.
 * @property shouldShowClearChatConfirmation True if should show the clear chat confirmation, false otherwise
 * @property shouldShowHistoryRetentionConfirmation True if should show the chat history retention confirmation, false otherwise
 * @property shouldNavigateUp True if we should navigate to the previous screen, false otherwise
 * @property shouldShowCustomTimePicker True if we should show the custom time pickers, false otherwise
 */
data class ManageChatHistoryUIState(
    val selectedHistoryRetentionTimeOption: ChatHistoryRetentionOption = ChatHistoryRetentionOption.Disabled,
    @StringRes val confirmButtonStringId: Int = R.string.general_ok,
    val isConfirmButtonEnable: Boolean = false,
    val shouldShowClearChatConfirmation: Boolean = false,
    val shouldShowHistoryRetentionConfirmation: Boolean = false,
    val shouldNavigateUp: Boolean = false,
    val shouldShowCustomTimePicker: Boolean = false,
)
