package mega.privacy.android.app.presentation.meeting.managechathistory

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ChatHistoryRetentionOption
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ManageChatHistoryUIState
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.app.utils.Constants.DISABLED_RETENTION_TIME
import mega.privacy.android.app.utils.Constants.SECONDS_IN_DAY
import mega.privacy.android.app.utils.Constants.SECONDS_IN_MONTH_30
import mega.privacy.android.app.utils.Constants.SECONDS_IN_WEEK
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

    /**
     * Local variable that stores the chat room ID.
     */
    private var chatRoomId = savedStateHandle.get<Long>(CHAT_ROOM_ID_KEY) ?: MEGACHAT_INVALID_HANDLE

    private val _uiState = MutableStateFlow(ManageChatHistoryUIState())

    val uiState: StateFlow<ManageChatHistoryUIState> = _uiState

    private val _chatRoomUiState = MutableStateFlow<ChatRoom?>(null)

    /**
     * The chat room ui state.
     *
     * This should be removed once migrated to compose.
     */
    val chatRoomUiState = _chatRoomUiState.asStateFlow()

    private val _retentionTimeUiState = MutableStateFlow(0L)

    /**
     * The updated retention time ui state.
     *
     * This should be removed once migrated to compose.
     */
    val retentionTimeUiState = _retentionTimeUiState.asStateFlow()

    private var monitorChatRetentionTimeUpdateJob: Job? = null

    /**
     * Initialize the chat room
     */
    fun initializeChatRoom(chatId: Long?, email: String?) {
        chatId?.let {
            setChatRoomId(it)
        }

        if (chatRoomId != MEGACHAT_INVALID_HANDLE) {
            getChatRoom()
            return
        }

        if (email.isNullOrBlank()) {
            Timber.e("Cannot init view, contact's email is empty")
            navigateUp()
            return
        }

        getChatRoomByUser(email)
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
        savedStateHandle[CHAT_ROOM_ID_KEY] = chatId
        chatRoomId = savedStateHandle.get<Long>(CHAT_ROOM_ID_KEY) ?: MEGACHAT_INVALID_HANDLE
    }

    private fun setChatRoomUiState(chatRoom: ChatRoom?) {
        _chatRoomUiState.update { chatRoom }
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
        _retentionTimeUiState.update { value }
    }

    private fun navigateUp() {
        _uiState.update { it.copy(shouldNavigateUp = true) }
    }

    /**
     * Reset the UI state after the user has navigated up
     */
    fun onNavigatedUp() {
        _uiState.update { it.copy(shouldNavigateUp = false) }
    }

    /**
     * Clear chat history
     *
     * @param chatId The chat room ID
     */
    fun clearChatHistory(chatId: Long) {
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
    fun setChatRetentionTime(period: Long) {
        viewModelScope.launch {
            runCatching { setChatRetentionTimeUseCase(chatId = chatRoomId, period = period) }
                .onSuccess {
                    updateHistoryRetentionTimeConfirmation(
                        getOptionFromRetentionTime(
                            period
                        )
                    )
                }
                .onFailure { Timber.e("Error setting retention time", it) }
        }
    }

    /**
     * Update the chat history retention confirmation related state based on the selected option
     *
     * @param option The selected option
     */
    fun updateHistoryRetentionTimeConfirmation(option: ChatHistoryRetentionOption) {
        _uiState.update {
            val shouldEnableConfirmButton =
                getOptionFromRetentionTime(_retentionTimeUiState.value) != ChatHistoryRetentionOption.Disabled ||
                        option != ChatHistoryRetentionOption.Disabled
            it.copy(
                selectedHistoryRetentionTimeOption = option,
                confirmButtonStringId = getConfirmButtonStringId(option),
                isConfirmButtonEnable = shouldEnableConfirmButton
            )
        }
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
    fun showClearChatConfirmation() {
        _uiState.update { it.copy(shouldShowClearChatConfirmation = true) }
    }

    /**
     * Dismiss the clear chat confirmation
     */
    fun dismissClearChatConfirmation() {
        _uiState.update { it.copy(shouldShowClearChatConfirmation = false) }
    }

    /**
     * Decides whether we need to show the custom time picker or
     * set the new retention time based on the confirmed option
     *
     * @param option The confirmed selected option
     */
    fun onNewRetentionTimeConfirmed(option: ChatHistoryRetentionOption) {
        if (option == ChatHistoryRetentionOption.Custom) {
            _uiState.update { it.copy(shouldShowCustomTimePicker = true) }
            return
        }

        setChatRetentionTime(period = getSecondsFromRetentionTimeOption(option))
    }

    private fun getSecondsFromRetentionTimeOption(option: ChatHistoryRetentionOption) =
        when (option) {
            ChatHistoryRetentionOption.OneDay -> SECONDS_IN_DAY.toLong()
            ChatHistoryRetentionOption.OneWeek -> SECONDS_IN_WEEK.toLong()
            ChatHistoryRetentionOption.OneMonth -> SECONDS_IN_MONTH_30.toLong()
            else -> DISABLED_RETENTION_TIME
        }

    /**
     * Reset the visibility state of the custom time picker
     */
    fun onCustomTimePickerSet() {
        _uiState.update { it.copy(shouldShowCustomTimePicker = false) }
    }

    /**
     * Show the history retention confirmation
     */
    fun showHistoryRetentionConfirmation() {
        _uiState.update {
            val selectedOption = getOptionFromRetentionTime(_retentionTimeUiState.value)
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
    fun dismissHistoryRetentionConfirmation() {
        _uiState.update { it.copy(shouldShowHistoryRetentionConfirmation = false) }
    }

    companion object {
        private const val CHAT_ROOM_ID_KEY = "CHAT_ROOM_ID_KEY"
    }
}
