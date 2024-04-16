package mega.privacy.android.app.presentation.meeting.managechathistory.model

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.chat.ChatRoom

/**
 * UI state for manage chat history screen.
 *
 * @property chatRoom The current [ChatRoom].
 * @property retentionTime The chat history retention time.
 * @property selectedHistoryRetentionTimeOption The current selected chat history retention time option.
 * @property confirmButtonStringId The string resource ID for the confirm button text
 * @property isConfirmButtonEnable True if should enable the confirm button of the history retention confirmation, false otherwise.
 * @property shouldShowClearChatConfirmation True if should show the clear chat confirmation, false otherwise
 * @property shouldShowHistoryRetentionConfirmation True if should show the chat history retention confirmation, false otherwise
 * @property shouldNavigateUp True if we should navigate to the previous screen, false otherwise
 * @property shouldShowCustomTimePicker True if we should show the custom time pickers, false otherwise
 * @property ordinalTimePickerItem Store custom time picker state related to the ordinal picker
 * @property periodTimePickerItem Store custom time picker state related to the period picker
 * @property isHistoryClearingOptionChecked True if the history clearing option is checked, false otherwise
 */
data class ManageChatHistoryUIState(
    val chatRoom: ChatRoom? = null,
    val retentionTime: Long = 0L,
    val selectedHistoryRetentionTimeOption: ChatHistoryRetentionOption = ChatHistoryRetentionOption.Disabled,
    @StringRes val confirmButtonStringId: Int = R.string.general_ok,
    val isConfirmButtonEnable: Boolean = false,
    val shouldShowClearChatConfirmation: Boolean = false,
    val shouldShowHistoryRetentionConfirmation: Boolean = false,
    val shouldNavigateUp: Boolean = false,
    val shouldShowCustomTimePicker: Boolean = false,
    val ordinalTimePickerItem: TimePickerItemUiState = TimePickerItemUiState(),
    val periodTimePickerItem: TimePickerItemUiState = TimePickerItemUiState(),
    val isHistoryClearingOptionChecked: Boolean = false,
)

/**
 * UI state for custom time picker in ManageChatHistoryScreen
 *
 * @property minimumWidth Minimum width of the picker
 * @property minimumValue Minimum value of the picker
 * @property maximumValue Maximum value of the picker
 * @property currentValue Current value of the picker
 * @property displayValues Values to be displayed by the picker
 */
@Immutable
data class TimePickerItemUiState(
    val minimumWidth: Int = 0,
    val minimumValue: Int = 0,
    val maximumValue: Int = 0,
    val currentValue: Int = 0,
    val displayValues: List<DisplayValueUiState>? = null,
)

/**
 * UI state for the custom time picker's display value
 */
@Stable
sealed interface DisplayValueUiState {

    /**
     * Singular string state.
     *
     * @property id The singular string ID for the display value
     */
    data class SingularString(@StringRes val id: Int) : DisplayValueUiState

    /**
     * Plural string state.
     *
     * @property id The plural string ID for the display value
     * @property quantity The quantity corresponds to the plural string
     */
    data class PluralString(
        @PluralsRes val id: Int,
        val quantity: Int = 0,
    ) : DisplayValueUiState
}
