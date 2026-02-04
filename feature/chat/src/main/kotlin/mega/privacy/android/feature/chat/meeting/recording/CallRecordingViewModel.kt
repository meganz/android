package mega.privacy.android.feature.chat.meeting.recording

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.domain.entity.call.CallRecordingConsentStatus
import mega.privacy.android.domain.usecase.call.BroadcastCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallByChatIdUseCase
import mega.privacy.android.domain.usecase.call.MonitorCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.call.MonitorCallSessionOnRecordingUseCase
import mega.privacy.android.domain.usecase.chat.MonitorCallInChatUseCase
import mega.privacy.android.feature.chat.meeting.call.isJoined
import mega.privacy.android.feature.chat.meeting.recording.model.CallRecordingUIState
import mega.privacy.android.navigation.destination.ChatNavKey
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for call recording
 *
 * @property state [mega.privacy.android.feature.chat.meeting.recording.model.CallRecordingUIState]
 */
@HiltViewModel
class CallRecordingViewModel @Inject constructor(
    private val monitorCallSessionOnRecordingUseCase: MonitorCallSessionOnRecordingUseCase,
    private val hangChatCallByChatIdUseCase: HangChatCallByChatIdUseCase,
    private val broadcastCallRecordingConsentEventUseCase: BroadcastCallRecordingConsentEventUseCase,
    private val monitorCallRecordingConsentEventUseCase: MonitorCallRecordingConsentEventUseCase,
    private val monitorCallInChatUseCase: MonitorCallInChatUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(CallRecordingUIState())
    val state = _state.asStateFlow()

    private var chatId: Long? = savedStateHandle[ChatNavKey.LEGACY_CHAT_ID]

    private var monitorCallSessionOnRecordingJob: Job? = null
    private var monitorCallInChatJob: Job? = null

    init {
        chatId?.let {
            monitorCallSessionOnRecording(it)
            monitorCallInChat(it)
        }
        viewModelScope.launch {
            monitorCallRecordingConsentEventUseCase().collectLatest { callRecordingConsentStatus ->
                _state.update { state ->
                    state.copy(callRecordingConsentStatus = callRecordingConsentStatus)
                }
            }
        }
    }

    /**
     * Sets chatId.
     */
    fun setChatId(chatId: Long) {
        if (chatId == this.chatId) {
            return
        }

        this.chatId = chatId
        monitorCallSessionOnRecording(chatId)
        monitorCallInChat(chatId)
    }

    /**
     * Monitors call session on recording.
     */
    private fun monitorCallSessionOnRecording(chatId: Long) {
        monitorCallSessionOnRecordingJob?.cancel()
        monitorCallSessionOnRecordingJob = viewModelScope.launch {
            monitorCallSessionOnRecordingUseCase(chatId)
                .catch { Timber.d(it) }
                .collectLatest { callRecordingEvent ->
                    callRecordingEvent?.let {
                        _state.update { state ->
                            state.copy(callRecordingEvent = callRecordingEvent)
                        }
                        if (!it.isSessionOnRecording) {
                            broadcastCallRecordingConsentEvent(CallRecordingConsentStatus.None)
                        }
                    }
                }
        }
    }

    private fun monitorCallInChat(chatId: Long) {
        monitorCallInChatJob?.cancel()
        monitorCallInChatJob = viewModelScope.launch {
            monitorCallInChatUseCase(chatId)
                .catch { Timber.d(it) }
                .collectLatest { call ->
                    _state.update { state ->
                        call?.let {
                            val isParticipatingInCall = call.status?.isJoined == true
                            val isRecording = call.sessionByClientId
                                .filter { it.value.isRecording }.isNotEmpty()

                            if (!isRecording || !isParticipatingInCall) {
                                broadcastCallRecordingConsentEvent(CallRecordingConsentStatus.None)
                            }

                            state.copy(
                                callRecordingEvent = state.callRecordingEvent.copy(
                                    isSessionOnRecording = isRecording
                                ),
                                isParticipatingInCall = isParticipatingInCall,
                            )
                        } ?: CallRecordingUIState()
                    }
                }
        }
    }

    /**
     * Sets participantRecording as null.
     */
    fun setParticipantRecordingConsumed() {
        _state.update { state ->
            state.copy(
                callRecordingEvent = state.callRecordingEvent.copy(participantRecording = null)
            )
        }
    }

    /**
     * Sets isRecordingConsentAccepted.
     */
    fun setIsRecordingConsentAccepted(accepted: Boolean) {
        chatId?.let { chatId ->
            val status =
                if (accepted) CallRecordingConsentStatus.Granted(chatId) else CallRecordingConsentStatus.Denied(
                    chatId
                )
            broadcastCallRecordingConsentEvent(status)

            if (status is CallRecordingConsentStatus.Denied) {
                viewModelScope.launch {
                    runCatching { hangChatCallByChatIdUseCase(chatId) }
                        .onFailure { Timber.d(it) }
                }
            }
        }
    }

    private fun broadcastCallRecordingConsentEvent(accepted: CallRecordingConsentStatus) {
        viewModelScope.launch {
            broadcastCallRecordingConsentEventUseCase(accepted)
        }
    }
}