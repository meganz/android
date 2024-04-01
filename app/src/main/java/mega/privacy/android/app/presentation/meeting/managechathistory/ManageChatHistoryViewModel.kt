package mega.privacy.android.app.presentation.meeting.managechathistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ManageChatHistoryUIState
import mega.privacy.android.app.presentation.snackbar.MegaSnackbarDuration
import mega.privacy.android.app.presentation.snackbar.SnackBarHandler
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRetentionTimeUpdateUseCase
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
    private val snackBarHandler: SnackBarHandler,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageChatHistoryUIState())

    val uiState: StateFlow<ManageChatHistoryUIState> = _uiState

    /**
     * Monitor chat retention time update.
     *
     * @param chatId The chat id to monitor.
     */
    fun monitorChatRetentionTimeUpdate(chatId: Long) {
        viewModelScope.launch {
            monitorChatRetentionTimeUpdateUseCase(chatId).collectLatest { retentionTime ->
                _uiState.update { state -> state.copy(retentionTimeUpdate = retentionTime) }
            }
        }
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
}
