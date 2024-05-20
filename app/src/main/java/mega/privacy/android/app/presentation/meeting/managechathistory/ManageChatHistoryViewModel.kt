package mega.privacy.android.app.presentation.meeting.managechathistory

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
import mega.privacy.android.app.presentation.chat.mapper.ChatRoomUiMapper
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ManageChatHistoryUIState
import mega.privacy.android.app.presentation.meeting.managechathistory.navigation.ManageChatHistoryArgs
import mega.privacy.android.app.utils.Constants
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
    private val chatRoomUiMapper: ChatRoomUiMapper,
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
        savedStateHandle[Constants.CHAT_ID] = chatId
        chatRoomId =
            savedStateHandle.get<Long>(Constants.CHAT_ID) ?: MEGACHAT_INVALID_HANDLE
    }

    private fun setChatRoomUiState(chatRoom: ChatRoom?) {
        val mappedChatRoom = if (chatRoom != null) chatRoomUiMapper(chatRoom) else null
        _uiState.update { it.copy(chatRoom = mappedChatRoom) }
    }

    private fun monitorChatRetentionTimeUpdate() {
        monitorChatRetentionTimeUpdateJob?.cancel()
        monitorChatRetentionTimeUpdateJob = viewModelScope.launch {
            monitorChatRetentionTimeUpdateUseCase(chatRoomId).collectLatest { retentionTime ->
                updateRetentionTimeState(retentionTime)
            }
        }
    }

    internal fun updateRetentionTimeState(value: Long) {
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
                    showStatusMessage(R.string.clear_history_success)
                }
                .onFailure {
                    Timber.e("Error clearing history", it)
                    showStatusMessage(R.string.clear_history_error)
                }
        }
    }

    private fun showStatusMessage(messageId: Int) {
        _uiState.update { it.copy(statusMessageResId = messageId) }
    }

    /**
     * Reset the error message
     */
    internal fun onStatusMessageDisplayed() {
        _uiState.update { it.copy(statusMessageResId = null) }
    }

    /**
     * Update the chat retention time
     *
     * @param period Retention timeframe in seconds
     */
    internal fun setChatRetentionTime(period: Long) {
        viewModelScope.launch {
            runCatching { setChatRetentionTimeUseCase(chatId = chatRoomId, period = period) }
                .onFailure { Timber.e("Error setting retention time", it) }
        }
    }
}
