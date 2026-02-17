package mega.privacy.android.feature.chat.meeting.recording

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.call.CallRecordingConsentStatus
import mega.privacy.android.domain.usecase.call.BroadcastCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallByChatIdUseCase
import mega.privacy.android.domain.usecase.call.MonitorCallRecordingConsentEventUseCase
import mega.privacy.android.feature.chat.meeting.recording.model.CallRecordingConsentUiState
import mega.privacy.android.navigation.contract.viewmodel.asUiStateFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * Call recording consent dialog view model
 *
 * @property monitorCallRecordingConsentEventUseCase
 * @property broadcastCallRecordingConsentEventUseCase
 * @property hangChatCallByChatIdUseCase
 */
@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class CallRecordingConsentDialogViewModel @Inject constructor(
    private val monitorCallRecordingConsentEventUseCase: MonitorCallRecordingConsentEventUseCase,
    private val broadcastCallRecordingConsentEventUseCase: BroadcastCallRecordingConsentEventUseCase,
    private val hangChatCallByChatIdUseCase: HangChatCallByChatIdUseCase,
) : ViewModel() {

    private var displayedChatId: Long? = null

    /**
     * State
     */
    val state: StateFlow<CallRecordingConsentUiState> by lazy {
        monitorCallRecordingConsentEventUseCase()
            .filter {
                if (it is CallRecordingConsentStatus.Requested) {
                    it.chatId != displayedChatId
                } else {
                    true
                }
            }
            .mapLatest { consentState ->
                when (consentState) {
                    is CallRecordingConsentStatus.Pending -> CallRecordingConsentUiState.ConsentRequired(
                        consentState.chatId
                    )

                    else -> CallRecordingConsentUiState.ConsentAlreadyHandled
                }
            }.onEach { uiState ->
                (uiState as? CallRecordingConsentUiState.ConsentRequired)?.let {
                    displayedChatId = it.chatId
                }
            }
            .asUiStateFlow(viewModelScope, CallRecordingConsentUiState.Loading)
    }

    /**
     * Accept
     *
     * @param chatId
     */
    fun accept(chatId: Long) {
        viewModelScope.launch {
            broadcastCallRecordingConsentEventUseCase(CallRecordingConsentStatus.Granted(chatId))
        }
    }

    /**
     * Decline
     *
     * @param chatId
     */
    fun decline(chatId: Long) {
        viewModelScope.launch {
            runCatching { hangChatCallByChatIdUseCase(chatId) }
                .onFailure { Timber.d(it) }
            broadcastCallRecordingConsentEventUseCase(CallRecordingConsentStatus.Denied(chatId))
        }
    }

    /**
     * On displayed
     *
     * @param chatId
     */
    fun onDisplayed(chatId: Long) {
        viewModelScope.launch {
            broadcastCallRecordingConsentEventUseCase(CallRecordingConsentStatus.Requested(chatId))
        }
    }
}