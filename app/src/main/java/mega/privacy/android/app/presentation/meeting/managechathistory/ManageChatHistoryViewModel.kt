package mega.privacy.android.app.presentation.meeting.managechathistory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.meeting.managechathistory.model.ManageChatHistoryUIState
import mega.privacy.android.domain.usecase.chat.MonitorChatRetentionTimeUpdateUseCase
import javax.inject.Inject

/**
 * ViewModel to manage chat history.
 *
 * @property state The state of the UI.
 */
@HiltViewModel
class ManageChatHistoryViewModel @Inject constructor(
    private val monitorChatRetentionTimeUpdateUseCase: MonitorChatRetentionTimeUpdateUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ManageChatHistoryUIState())

    val state: StateFlow<ManageChatHistoryUIState> = _state

    /**
     * Monitor chat retention time update.
     *
     * @param chatId The chat id to monitor.
     */
    fun monitorChatRetentionTimeUpdate(chatId: Long) {
        viewModelScope.launch {
            monitorChatRetentionTimeUpdateUseCase(chatId).collectLatest { retentionTime ->
                _state.update { state -> state.copy(retentionTimeUpdate = retentionTime) }
            }
        }
    }

    /**
     * Update retention time.
     */
    fun onRetentionTimeUpdateConsumed() {
        _state.update { state -> state.copy(retentionTimeUpdate = null) }
    }
}