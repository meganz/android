package mega.privacy.android.app.presentation.meeting.managechathistory

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
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ManageChatHistoryUIState
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
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
     * Local variable that stores the chat room ID
     */
    var chatRoomId = savedStateHandle.get<Long>(CHAT_ROOM_ID_KEY) ?: MEGACHAT_INVALID_HANDLE
        private set

    private val _uiState = MutableStateFlow(ManageChatHistoryUIState())

    val uiState: StateFlow<ManageChatHistoryUIState> = _uiState

    private val _chatRoomUiState = MutableStateFlow<ChatRoom?>(null)

    /**
     * The chat room ui state.
     */
    val chatRoomUiState = _chatRoomUiState.asStateFlow()

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
        chatRoom?.let { setChatRoomId(it.chatId) }
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
                _uiState.update { state -> state.copy(retentionTimeUpdate = retentionTime) }
            }
        }
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
     * Update retention time.
     */
    fun onRetentionTimeUpdateConsumed() {
        _uiState.update { state -> state.copy(retentionTimeUpdate = null) }
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
     * @param chatId Chat room ID
     * @param period Retention timeframe in seconds
     */
    fun setChatRetentionTime(chatId: Long, period: Long) {
        viewModelScope.launch {
            runCatching { setChatRetentionTimeUseCase(chatId = chatId, period = period) }
                .onFailure { Timber.e("Error setting retention time", it) }
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

    companion object {
        private const val CHAT_ROOM_ID_KEY = "CHAT_ROOM_ID_KEY"
    }
}
