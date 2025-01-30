package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.meeting.activity.MeetingActivity.Companion.MEETING_CHAT_ID
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.presentation.meeting.model.CallRecordingUIState
import mega.privacy.android.app.presentation.meeting.model.RingingUIState
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.domain.entity.call.CallCompositionChanges
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.meeting.FakeIncomingCallState
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.call.BroadcastCallEndedUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallUseCase
import mega.privacy.android.domain.usecase.call.IsChatStatusConnectedForCallUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatConnectionStateUseCase
import mega.privacy.android.domain.usecase.meeting.GetCallAvatarUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRoomUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.SetFakeIncomingCallStateUseCase
import mega.privacy.android.domain.usecase.meeting.SetPendingToHangUpCallUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for call recording
 *
 * @property state [CallRecordingUIState]
 */
@HiltViewModel
class RingingViewModel @Inject constructor(
    private val getChatCallUseCase: GetChatCallUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val broadcastCallEndedUseCase: BroadcastCallEndedUseCase,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val getCallAvatarUseCase: GetCallAvatarUseCase,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
    private val setFakeIncomingCallStateUseCase: SetFakeIncomingCallStateUseCase,
    private val isChatStatusConnectedForCallUseCase: IsChatStatusConnectedForCallUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val monitorChatRoomUpdatesUseCase: MonitorChatRoomUpdatesUseCase,
    private val monitorChatConnectionStateUseCase: MonitorChatConnectionStateUseCase,
    private val setPendingToHangUpCallUseCase: SetPendingToHangUpCallUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        RingingUIState(
            chatId = savedStateHandle[MEETING_CHAT_ID]
                ?: -1L
        )
    )

    val state = _state.asStateFlow()

    private var monitorChatCallUpdatesJob: Job? = null
    private var monitorChatRoomUpdatesJob: Job? = null
    private var monitorChatConnectionStateJob: Job? = null

    init {
        viewModelScope.launch {
            runCatching {
                setFakeIncomingCallStateUseCase(
                    chatId = state.value.chatId,
                    type = FakeIncomingCallState.Screen
                )
            }
        }

        getMyUserHandle()
        getChatConnectionState()
        getChatRoom()
        getChatCall()
        startMonitorChatRoomUpdates()
        startMonitoringChatCallUpdates()
        startMonitorChatConnectionStatus()
    }

    /**
     * Get chat room
     */
    fun getChatRoom() {
        viewModelScope.launch {
            runCatching {
                getChatRoomUseCase(_state.value.chatId)
            }.onSuccess { chat ->
                chat?.let {
                    _state.update { state ->
                        state.copy(
                            chat = chat
                        )
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Get chat call
     */
    fun getChatCall() {
        viewModelScope.launch {
            runCatching {
                getChatCallUseCase(_state.value.chatId)
            }.onSuccess { call ->
                call?.let {
                    updateCall(it)
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Get chat room updates
     */
    private fun startMonitorChatRoomUpdates() {
        monitorChatRoomUpdatesJob?.cancel()
        monitorChatRoomUpdatesJob = viewModelScope.launch {
            monitorChatRoomUpdatesUseCase(state.value.chatId)
                .collectLatest { chat ->
                    _state.update { state ->
                        state.copy(
                            chat = chat
                        )
                    }
                }
        }
    }

    /**
     * Get chat connection state updates
     */
    private fun startMonitorChatConnectionStatus() {
        monitorChatConnectionStateJob?.cancel()
        monitorChatConnectionStateJob = viewModelScope.launch {
            monitorChatConnectionStateUseCase()
                .filter { it.chatId == _state.value.chatId }
                .collectLatest {
                    val isConnected = isChatStatusConnectedForCallUseCase(it.chatId)
                    if (isConnected && state.value.call == null) {
                        rtcAudioManagerGateway.stopSounds()
                        rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                        _state.update { state -> state.copy(finish = true) }
                    } else {
                        _state.update { state ->
                            state.copy(chatConnectionStatus = it.chatConnectionStatus)
                        }
                    }
                }
        }
    }

    /**
     * Get chat call updates
     */
    private fun startMonitoringChatCallUpdates() {
        monitorChatCallUpdatesJob?.cancel()
        monitorChatCallUpdatesJob = viewModelScope.launch {
            monitorChatCallUpdatesUseCase()
                .filter { it.chatId == _state.value.chatId }
                .collectLatest { call ->
                    updateCall(call)
                    call.changes?.apply {
                        when {
                            contains(ChatCallChanges.CallComposition) -> {
                                if (call.callCompositionChange == CallCompositionChanges.Added || call.callCompositionChange == CallCompositionChanges.Removed) {
                                    val numParticipants = call.numParticipants ?: 0
                                    if (call.callCompositionChange == CallCompositionChanges.Added && numParticipants > 1 &&
                                        state.value.myUserHandle == call.peerIdCallCompositionChange && call.status == ChatCallStatus.UserNoPresent
                                    ) {
                                        _state.update { state ->
                                            state.copy(
                                                callAnsweredInAnotherClient = true,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    /**
     * Update call
     *
     * @param call  [ChatCall]
     */
    private fun updateCall(call: ChatCall) {
        if (state.value.call == null) {
            call.caller?.let {
                if (!state.value.isCallAnsweredAndWaitingForCallInfo) {
                    getAvatar(it)
                }
            }
        }

        _state.update { state ->
            state.copy(
                call = call
            )
        }
    }

    /**
     * Get avatar
     *
     * @param callerHandle  Caller handle
     */
    private fun getAvatar(callerHandle: Long) {
        viewModelScope.launch {
            runCatching {
                getCallAvatarUseCase(state.value.chatId, callerHandle)
            }.onSuccess {
                _state.update { state ->
                    state.copy(
                        avatar = it
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * load my user handle and save to ui state
     */
    private fun getMyUserHandle() {
        viewModelScope.launch {
            runCatching {
                val myUserHandle = getMyUserHandleUseCase()
                _state.update { state -> state.copy(myUserHandle = myUserHandle) }
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Get chat connection state
     */
    private fun getChatConnectionState() {
        viewModelScope.launch {
            runCatching {
                isChatStatusConnectedForCallUseCase(chatId = state.value.chatId)
            }.onSuccess { connected ->
                if (connected) {
                    _state.update { state -> state.copy(chatConnectionStatus = ChatConnectionStatus.Online) }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Show snackbar
     */
    fun showSnackbar() {
        _state.update { state -> state.copy(showSnackbar = true) }
    }

    /**
     * Hide snackbar
     */
    fun hideSnackbar() {
        _state.update { state -> state.copy(showSnackbar = false) }
    }

    /**
     * Process when audio button is clicked
     *
     * @param isClicked True, if it's clicked
     */
    fun onAudioClicked(isClicked: Boolean) {
        if (isClicked) {
            rtcAudioManagerGateway.stopSounds()
        }

        _state.update { state ->
            state.copy(
                isAnswerWithAudioClicked = isClicked,
                isCallAnsweredAndWaitingForCallInfo = true
            )
        }
    }

    /**
     * Process when video button is clicked
     *
     * @param isClicked True, if it's clicked
     */
    fun onVideoClicked(isClicked: Boolean) {
        if (isClicked) {
            rtcAudioManagerGateway.stopSounds()
        }

        _state.update { state ->
            state.copy(
                isAnswerWithVideoClicked = isClicked,
                isCallAnsweredAndWaitingForCallInfo = true
            )
        }
    }

    /**
     * Process when hang up button is clicked but the call is not recovered
     **/
    fun onHangUpClicked() {
        rtcAudioManagerGateway.stopSounds()
        rtcAudioManagerGateway.removeRTCAudioManagerRingIn()

        state.value.call?.let { call ->
            Timber.d("Call exists")
            CallUtil.clearIncomingCallNotification(call.callId)
            hangUpCall(chatId = call.chatId, callId = call.callId)
        } ?: run {
            Timber.d("Call is null")
            viewModelScope.launch {
                runCatching {
                    setPendingToHangUpCallUseCase(
                        chatId = state.value.chatId,
                        add = true
                    )
                }
            }
        }

        _state.update { state -> state.copy(finish = true) }
    }

    /**
     * Hang up call
     *
     * @param chatId    Chat id
     * @param callId    Call id
     */
    private fun hangUpCall(chatId: Long, callId: Long) {
        viewModelScope.launch {
            runCatching {
                hangChatCallUseCase(callId)
            }.onSuccess {
                broadcastCallEndedUseCase(chatId)
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }
}