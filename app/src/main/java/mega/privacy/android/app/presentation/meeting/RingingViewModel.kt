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
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.call.BroadcastCallEndedUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallUseCase
import mega.privacy.android.domain.usecase.call.SetIgnoredCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetCallAvatarUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRoomUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
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
    private val setIgnoredCallUseCase: SetIgnoredCallUseCase,
    private val getCallAvatarUseCase: GetCallAvatarUseCase,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val broadcastCallEndedUseCase: BroadcastCallEndedUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val monitorChatRoomUpdatesUseCase: MonitorChatRoomUpdatesUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(RingingUIState())
    val state = _state.asStateFlow()

    private var chatId: Long? = savedStateHandle[MEETING_CHAT_ID]
    private var monitorChatCallUpdatesJob: Job? = null
    private var monitorChatRoomUpdatesJob: Job? = null

    init {
        getMyUserHandle()
        chatId?.let {
            _state.update { state ->
                state.copy(
                    chatId = it
                )
            }
            getChatRoom()
            getChatCall()
            startMonitorChatRoomUpdates()
        }

        startMonitoringChatCallUpdates()
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
                getAvatar(it)
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
        _state.update { state -> state.copy(isAnswerWithAudioClicked = isClicked) }
    }

    /**
     * Process when video button is clicked
     *
     * @param isClicked True, if it's clicked
     */
    fun onVideoClicked(isClicked: Boolean) {
        _state.update { state -> state.copy(isAnswerWithVideoClicked = isClicked) }

    }

    /**
     * Process when hang up button is clicked
     *
     * @param isClicked True, if it's clicked
     */
    fun onHangUpClicked(isClicked: Boolean) {
        _state.update { state -> state.copy(isHangUpClicked = isClicked) }

    }

    /**
     * Process hang up incoming call
     *
     * @param callId    Call id
     */
    fun processHangUp(callId: Long) {
        _state.update { state -> state.copy(isHangUpClicked = false) }

        rtcAudioManagerGateway.stopSounds()
        CallUtil.clearIncomingCallNotification(callId)
        when {
            state.value.isOneToOneCall == true -> hangCurrentCall()
            else -> ignoreCall()
        }
        _state.update { state -> state.copy(finish = true) }
    }

    /**
     * Ignore call
     */
    private fun ignoreCall() {
        viewModelScope.launch {
            runCatching {
                setIgnoredCallUseCase(_state.value.chatId)
            }.onSuccess {
                if (it) {
                    Timber.d("Call was ignored")
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Hang up the current call
     */
    private fun hangCurrentCall() {
        state.value.call?.apply {
            hangCall(callId)
        }
    }

    /**
     * Hang up a specified call
     *
     * @param callId Call ID
     */
    private fun hangCall(callId: Long) = viewModelScope.launch {
        Timber.d("Hang up call. Call id $callId")
        runCatching {
            hangChatCallUseCase(callId)
        }.onSuccess {
            broadcastCallEndedUseCase(state.value.chatId)
        }.onFailure {
            Timber.e(it.stackTraceToString())
        }
    }
}