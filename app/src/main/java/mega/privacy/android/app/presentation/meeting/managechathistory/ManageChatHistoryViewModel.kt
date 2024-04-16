package mega.privacy.android.app.presentation.meeting.managechathistory

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ChatHistoryRetentionOption
import mega.privacy.android.app.presentation.meeting.managechathistory.model.DisplayValueUiState
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ManageChatHistoryUIState
import mega.privacy.android.app.presentation.meeting.managechathistory.model.TimePickerItemUiState
import mega.privacy.android.app.presentation.meeting.managechathistory.navigation.ManageChatHistoryArgs
import mega.privacy.android.app.presentation.meeting.managechathistory.navigation.manageChatHistoryChatIdArg
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.app.utils.Constants.DISABLED_RETENTION_TIME
import mega.privacy.android.app.utils.Constants.SECONDS_IN_DAY
import mega.privacy.android.app.utils.Constants.SECONDS_IN_HOUR
import mega.privacy.android.app.utils.Constants.SECONDS_IN_MONTH_30
import mega.privacy.android.app.utils.Constants.SECONDS_IN_WEEK
import mega.privacy.android.app.utils.Constants.SECONDS_IN_YEAR
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.GetChatRoomByUserUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRetentionTimeUpdateUseCase
import mega.privacy.android.domain.usecase.chat.SetChatRetentionTimeUseCase
import mega.privacy.android.domain.usecase.contact.GetContactHandleUseCase
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel to manage chat history.
 *
 * @property uiState The state of the UI.
 */
@HiltViewModel
class ManageChatHistoryViewModel @Inject constructor(
    private val monitorChatRetentionTimeUpdateUseCase: MonitorChatRetentionTimeUpdateUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val setChatRetentionTimeUseCase: SetChatRetentionTimeUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val getContactHandleUseCase: GetContactHandleUseCase,
    private val getChatRoomByUserUseCase: GetChatRoomByUserUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val snackBarHandler: SnackBarHandler,
) : ViewModel() {

    private val manageChatHistoryArg = ManageChatHistoryArgs(savedStateHandle)

    /**
     * Local variable that stores the chat room ID.
     */
    private var chatRoomId = manageChatHistoryArg.chatId

    private val _uiState = MutableStateFlow(ManageChatHistoryUIState())

    val uiState: StateFlow<ManageChatHistoryUIState> = _uiState

    private var monitorChatRetentionTimeUpdateJob: Job? = null

    init {
        initializeChatRoom()
    }

    /**
     * Initialize the chat room
     */
    internal fun initializeChatRoom() {
        if (chatRoomId != MEGACHAT_INVALID_HANDLE) {
            getChatRoom()
            return
        }

        if (manageChatHistoryArg.email.isNullOrBlank()) {
            Timber.e("Cannot init view, contact's email is empty")
            navigateUp()
            return
        }

        getChatRoomByUser(manageChatHistoryArg.email)
    }

    private fun getChatRoom() {
        viewModelScope.launch {
            runCatching { getChatRoomUseCase(chatRoomId) }
                .onSuccess { updateAndMonitorChatRoom(it) }
                .onFailure { Timber.e("Failed to get chat room", it) }
        }
    }

    private fun getChatRoomByUser(email: String) {
        viewModelScope.launch {
            runCatching { getContactHandleUseCase(email) }
                .onSuccess { handle ->
                    if (handle == null) {
                        Timber.e("Cannot init view, contact is null")
                        navigateUp()
                    } else {
                        runCatching { getChatRoomByUserUseCase(handle) }
                            .onSuccess { updateAndMonitorChatRoom(it) }
                            .onFailure { Timber.e("Failed to get chat room by user", it) }
                    }
                }
                .onFailure { Timber.e("Failed to get contact's handle", it) }
        }
    }

    private fun updateAndMonitorChatRoom(chatRoom: ChatRoom?) {
        chatRoom?.let {
            setChatRoomId(it.chatId)
            updateRetentionTimeState(it.retentionTime)
        }
        monitorChatRetentionTimeUpdate()
        setChatRoomUiState(chatRoom)
    }

    private fun setChatRoomId(chatId: Long) {
        savedStateHandle[manageChatHistoryChatIdArg] = chatId
        chatRoomId =
            savedStateHandle.get<Long>(manageChatHistoryChatIdArg) ?: MEGACHAT_INVALID_HANDLE
    }

    private fun setChatRoomUiState(chatRoom: ChatRoom?) {
        _uiState.update { it.copy(chatRoom = chatRoom) }
    }

    private fun monitorChatRetentionTimeUpdate() {
        monitorChatRetentionTimeUpdateJob?.cancel()
        monitorChatRetentionTimeUpdateJob = viewModelScope.launch {
            monitorChatRetentionTimeUpdateUseCase(chatRoomId).collectLatest { retentionTime ->
                updateHistoryRetentionTimeConfirmation(
                    getOptionFromRetentionTime(
                        retentionTime
                    )
                )
                updateRetentionTimeState(retentionTime)
            }
        }
    }

    private fun updateRetentionTimeState(value: Long) {
        _uiState.update { it.copy(retentionTime = value) }
    }

    private fun navigateUp() {
        _uiState.update { it.copy(shouldNavigateUp = true) }
    }

    /**
     * Reset the UI state after the user has navigated up
     */
    internal fun onNavigatedUp() {
        _uiState.update { it.copy(shouldNavigateUp = false) }
    }

    /**
     * Clear chat history
     *
     * @param chatId The chat room ID
     */
    internal fun clearChatHistory(chatId: Long) {
        viewModelScope.launch {
            runCatching { clearChatHistoryUseCase(chatId) }
                .onSuccess {
                    snackBarHandler.postSnackbarMessage(
                        resId = R.string.clear_history_success,
                        snackbarDuration = MegaSnackbarDuration.Long,
                    )
                }
                .onFailure {
                    Timber.e("Error clearing history", it)
                    snackBarHandler.postSnackbarMessage(
                        resId = R.string.clear_history_error,
                        snackbarDuration = MegaSnackbarDuration.Long
                    )
                }
        }
    }

    /**
     * Update the chat retention time
     *
     * @param period Retention timeframe in seconds
     */
    internal fun setChatRetentionTime(period: Long) {
        viewModelScope.launch {
            runCatching { setChatRetentionTimeUseCase(chatId = chatRoomId, period = period) }
                .onSuccess {
                    updateHistoryRetentionTimeConfirmation(
                        getOptionFromRetentionTime(
                            period
                        )
                    )

                    if (_uiState.value.shouldShowCustomTimePicker) {
                        hideCustomTimePicker()
                    }
                }
                .onFailure { Timber.e("Error setting retention time", it) }
        }
    }

    /**
     * Update the chat history retention confirmation related state based on the selected option
     *
     * @param option The selected option
     */
    internal fun updateHistoryRetentionTimeConfirmation(option: ChatHistoryRetentionOption) {
        _uiState.update {
            val shouldEnableConfirmButton =
                getOptionFromRetentionTime(_uiState.value.retentionTime) != ChatHistoryRetentionOption.Disabled ||
                        option != ChatHistoryRetentionOption.Disabled
            it.copy(
                selectedHistoryRetentionTimeOption = option,
                confirmButtonStringId = getConfirmButtonStringId(option),
                isConfirmButtonEnable = shouldEnableConfirmButton
            )
        }
    }

    /**
     * Reset the visibility state of the custom time picker
     */
    internal fun hideCustomTimePicker() {
        _uiState.update { it.copy(shouldShowCustomTimePicker = false) }
    }

    private fun getOptionFromRetentionTime(period: Long): ChatHistoryRetentionOption {
        if (period == DISABLED_RETENTION_TIME) {
            return ChatHistoryRetentionOption.Disabled
        }

        val days = period % SECONDS_IN_DAY
        val weeks = period % SECONDS_IN_WEEK
        val months = period % SECONDS_IN_MONTH_30

        val isOneMonthPeriod = period / SECONDS_IN_MONTH_30 == 1L
        val isOneWeekPeriod = period / SECONDS_IN_WEEK == 1L
        val isOneDayPeriod = period / SECONDS_IN_DAY == 1L

        return when {
            months == 0L && isOneMonthPeriod -> ChatHistoryRetentionOption.OneMonth

            weeks == 0L && isOneWeekPeriod -> ChatHistoryRetentionOption.OneWeek

            days == 0L && isOneDayPeriod -> ChatHistoryRetentionOption.OneDay

            else -> ChatHistoryRetentionOption.Custom
        }
    }

    /**
     * Show clear chat confirmation
     */
    internal fun showClearChatConfirmation() {
        _uiState.update { it.copy(shouldShowClearChatConfirmation = true) }
    }

    /**
     * Dismiss the clear chat confirmation
     */
    internal fun dismissClearChatConfirmation() {
        _uiState.update { it.copy(shouldShowClearChatConfirmation = false) }
    }

    /**
     * Decides whether we need to show the custom time picker or
     * set the new retention time based on the confirmed option
     *
     * @param option The confirmed selected option
     */
    internal fun onNewRetentionTimeOptionConfirmed(option: ChatHistoryRetentionOption) {
        if (option == ChatHistoryRetentionOption.Custom) {
            _uiState.update {
                val (ordinalTimePickerItem, periodTimePickerItem) = getInitialTimePickerItems()
                it.copy(
                    shouldShowCustomTimePicker = true,
                    ordinalTimePickerItem = ordinalTimePickerItem,
                    periodTimePickerItem = periodTimePickerItem
                )
            }
            return
        }

        setChatRetentionTime(period = getSecondsFromRetentionTimeOption(option))
    }

    /**
     * Get the initial time picker property for the UI
     *
     * @return Pair of the ordinal and the period time picker UI states
     */
    private fun getInitialTimePickerItems(): Pair<TimePickerItemUiState, TimePickerItemUiState> {
        var ordinalMaximumValue = MAXIMUM_VALUE_ORDINAL_PICKER_HOURS
        var ordinalCurrentValue = MINIMUM_VALUE_ORDINAL_PICKER
        var periodCurrentValue = OPTION_HOURS

        when {
            _uiState.value.retentionTime == DISABLED_RETENTION_TIME -> {
                periodCurrentValue = MINIMUM_VALUE_PERIOD_PICKER
            }

            _uiState.value.retentionTime.isYearsOnly() -> {
                periodCurrentValue = OPTION_YEARS
                ordinalMaximumValue = MINIMUM_VALUE_ORDINAL_PICKER

                val totalYears = _uiState.value.retentionTime / SECONDS_IN_YEAR
                ordinalCurrentValue = totalYears.toInt()
            }

            _uiState.value.retentionTime.isMonthsOnly() -> {
                periodCurrentValue = OPTION_MONTHS
                ordinalMaximumValue = MAXIMUM_VALUE_ORDINAL_PICKER_MONTHS

                val totalMonths = _uiState.value.retentionTime / SECONDS_IN_MONTH_30
                ordinalCurrentValue = totalMonths.toInt()
            }

            _uiState.value.retentionTime.isWeeksOnly() -> {
                periodCurrentValue = OPTION_WEEKS
                ordinalMaximumValue = MAXIMUM_VALUE_ORDINAL_PICKER_WEEKS

                val totalWeeks = _uiState.value.retentionTime / SECONDS_IN_WEEK
                ordinalCurrentValue = totalWeeks.toInt()
            }

            _uiState.value.retentionTime.isDaysOnly() -> {
                periodCurrentValue = OPTION_DAYS
                ordinalMaximumValue = MAXIMUM_VALUE_ORDINAL_PICKER_DAYS

                val totalDays = _uiState.value.retentionTime / SECONDS_IN_DAY
                ordinalCurrentValue = totalDays.toInt()
            }

            _uiState.value.retentionTime.isHoursOnly() -> {
                val totalHours = _uiState.value.retentionTime / SECONDS_IN_HOUR
                ordinalCurrentValue = totalHours.toInt()
            }
        }

        val ordinalPickerUiState = TimePickerItemUiState(
            minimumValue = MINIMUM_VALUE_ORDINAL_PICKER,
            maximumValue = ordinalMaximumValue,
            currentValue = ordinalCurrentValue
        )
        val periodPickerUiState = TimePickerItemUiState(
            minimumWidth = MAXIMUM_VALUE_PERIOD_PICKER,
            minimumValue = MINIMUM_VALUE_PERIOD_PICKER,
            maximumValue = MAXIMUM_VALUE_PERIOD_PICKER,
            currentValue = periodCurrentValue,
            displayValues = getDisplayValues(ordinalCurrentValue)
        )
        return Pair(ordinalPickerUiState, periodPickerUiState)
    }

    private fun Long.isYearsOnly() = (this % SECONDS_IN_YEAR) == 0L

    private fun Long.isMonthsOnly() = (this % SECONDS_IN_MONTH_30) == 0L

    private fun Long.isWeeksOnly() = (this % SECONDS_IN_WEEK) == 0L

    private fun Long.isDaysOnly() = (this % SECONDS_IN_DAY) == 0L

    private fun Long.isHoursOnly() = (this % SECONDS_IN_HOUR) == 0L

    private fun getSecondsFromRetentionTimeOption(option: ChatHistoryRetentionOption) =
        when (option) {
            ChatHistoryRetentionOption.OneDay -> SECONDS_IN_DAY.toLong()
            ChatHistoryRetentionOption.OneWeek -> SECONDS_IN_WEEK.toLong()
            ChatHistoryRetentionOption.OneMonth -> SECONDS_IN_MONTH_30.toLong()
            else -> DISABLED_RETENTION_TIME
        }

    internal fun onCustomTimePickerConfirmed() {
        val totalSeconds =
            getPeriodPickerTotalSeconds() * _uiState.value.ordinalTimePickerItem.currentValue
        setChatRetentionTime(period = totalSeconds.toLong())
    }

    private fun getPeriodPickerTotalSeconds() =
        when (_uiState.value.periodTimePickerItem.currentValue) {
            OPTION_HOURS -> SECONDS_IN_HOUR
            OPTION_DAYS -> SECONDS_IN_DAY
            OPTION_WEEKS -> SECONDS_IN_WEEK
            OPTION_MONTHS -> SECONDS_IN_MONTH_30
            else -> SECONDS_IN_YEAR
        }

    /**
     * Show the history retention confirmation
     */
    internal fun showHistoryRetentionConfirmation() {
        _uiState.update {
            val selectedOption = getOptionFromRetentionTime(_uiState.value.retentionTime)
            it.copy(
                shouldShowHistoryRetentionConfirmation = true,
                selectedHistoryRetentionTimeOption = selectedOption,
                confirmButtonStringId = getConfirmButtonStringId(selectedOption),
                isConfirmButtonEnable = selectedOption != ChatHistoryRetentionOption.Disabled
            )
        }
    }

    @StringRes
    private fun getConfirmButtonStringId(option: ChatHistoryRetentionOption): Int {
        return if (option == ChatHistoryRetentionOption.Custom) {
            R.string.general_next
        } else {
            R.string.general_ok
        }
    }

    /**
     * Dismiss the history retention confirmation
     */
    internal fun dismissHistoryRetentionConfirmation() {
        _uiState.update { it.copy(shouldShowHistoryRetentionConfirmation = false) }
    }

    private fun getDisplayValues(value: Int) = listOf(
        DisplayValueUiState.PluralString(
            id = R.plurals.retention_time_picker_hours,
            quantity = value
        ),
        DisplayValueUiState.PluralString(
            id = R.plurals.retention_time_picker_days,
            quantity = value
        ),
        DisplayValueUiState.PluralString(
            id = R.plurals.retention_time_picker_weeks,
            quantity = value
        ),
        DisplayValueUiState.PluralString(
            id = R.plurals.retention_time_picker_months,
            quantity = value
        ),
        DisplayValueUiState.SingularString(
            id = R.string.retention_time_picker_year
        )
    )

    companion object {
        private const val OPTION_HOURS = 0
        private const val OPTION_DAYS = 1
        private const val OPTION_MONTHS = 3
        private const val OPTION_WEEKS = 2
        private const val OPTION_YEARS = 4

        private const val MINIMUM_VALUE_ORDINAL_PICKER = 1
        private const val MAXIMUM_VALUE_ORDINAL_PICKER_HOURS = 24
        private const val MAXIMUM_VALUE_ORDINAL_PICKER_DAYS = 31
        private const val MAXIMUM_VALUE_ORDINAL_PICKER_WEEKS = 4
        private const val MAXIMUM_VALUE_ORDINAL_PICKER_MONTHS = 12
        private const val MINIMUM_VALUE_PERIOD_PICKER = 0
        private const val MAXIMUM_VALUE_PERIOD_PICKER = 4
    }
}
