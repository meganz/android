package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.meeting.model.CallRecordingUIState
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.usecase.meeting.BroadcastCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.meeting.HangChatCallByChatIdUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallFinishedByChatIdUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallSessionOnRecordingUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for call recording
 *
 * @property state [CallRecordingUIState]
 */
@HiltViewModel
class CallRecordingViewModel @Inject constructor(
    private val monitorCallSessionOnRecordingUseCase: MonitorCallSessionOnRecordingUseCase,
    private val hangChatCallByChatIdUseCase: HangChatCallByChatIdUseCase,
    private val broadcastCallRecordingConsentEventUseCase: BroadcastCallRecordingConsentEventUseCase,
    private val monitorCallRecordingConsentEventUseCase: MonitorCallRecordingConsentEventUseCase,
    private val monitorCallFinishedByChatIdUseCase: MonitorCallFinishedByChatIdUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(CallRecordingUIState())
    val state = _state.asStateFlow()

    private var chatId: Long? = savedStateHandle[Constants.CHAT_ID]

    private var monitorCallSessionOnRecordingJob: Job? = null
    private var monitorCallHungUpByChatIdJob: Job? = null

    init {
        chatId?.let {
            monitorCallSessionOnRecording(it)
            monitorCallFinishedByChatById(it)
        }
        viewModelScope.launch {
            monitorCallRecordingConsentEventUseCase().collectLatest { isRecordingConsentAccepted ->
                _state.update { state ->
                    if (isRecordingConsentAccepted == null) {
                        CallRecordingUIState()
                    } else {
                        state.copy(isRecordingConsentAccepted = isRecordingConsentAccepted)
                    }
                }
            }
        }
    }

    /**
     * Sets chatId.
     */
    fun setChatId(chatId: Long) {
        this.chatId = chatId
        monitorCallSessionOnRecording(chatId)
        monitorCallFinishedByChatById(chatId)
    }

    /**
     * Monitors call session on recording.
     */
    private fun monitorCallSessionOnRecording(chatId: Long) {
        monitorCallSessionOnRecordingJob?.cancel()
        monitorCallSessionOnRecordingJob = viewModelScope.launch {
            monitorCallSessionOnRecordingUseCase(chatId).collectLatest { callRecordingEvent ->
                callRecordingEvent?.let {
                    _state.update { state ->
                        if (callRecordingEvent.isSessionOnRecording) {
                            state.copy(callRecordingEvent = callRecordingEvent)
                        } else {
                            state.copy(
                                callRecordingEvent = callRecordingEvent,
                                isRecordingConsentAccepted = null
                            )
                        }
                    }
                }
            }
        }
    }

    private fun monitorCallFinishedByChatById(chatId: Long) {
        monitorCallHungUpByChatIdJob?.cancel()
        monitorCallHungUpByChatIdJob = viewModelScope.launch {
            monitorCallFinishedByChatIdUseCase(chatId)
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
        _state.update { state ->
            state.copy(isRecordingConsentAccepted = accepted)
        }
        broadcastCallRecordingConsentEvent(accepted)
        if (!accepted) {
            chatId?.let { chatId ->
                viewModelScope.launch {
                    runCatching { hangChatCallByChatIdUseCase(chatId) }
                        .onFailure { Timber.d(it) }
                }
            }
        }
    }

    private fun broadcastCallRecordingConsentEvent(accepted: Boolean?) {
        viewModelScope.launch {
            broadcastCallRecordingConsentEventUseCase(accepted)
        }
    }
}