package mega.privacy.android.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.gateway.CameraGateway
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.chat.model.ChatState
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.extensions.isPast
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.ChatRequestParamType
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.MonitorConnectivity
import mega.privacy.android.domain.usecase.MonitorStorageStateEvent
import mega.privacy.android.domain.usecase.meeting.AnswerChatCall
import mega.privacy.android.domain.usecase.meeting.GetChatCall
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import mega.privacy.android.domain.usecase.meeting.StartChatCall
import mega.privacy.android.domain.usecase.meeting.StartChatCallNoRinging
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.main.megachat.ChatActivity]
 *
 * @property monitorStorageStateEvent           [MonitorStorageStateEvent]
 * @property startChatCall                      [StartChatCall]
 * @property chatApiGateway                     [MegaChatApiGateway]
 * @property answerChatCall                     [AnswerChatCall]
 * @property passcodeManagement                 [PasscodeManagement]
 * @property cameraGateway                      [CameraGateway]
 * @property chatManagement                     [ChatManagement]
 * @property rtcAudioManagerGateway             [RTCAudioManagerGateway]
 * @property startChatCallNoRinging             [StartChatCallNoRinging]
 * @property megaChatApiGateway                 [MegaChatApiGateway]
 * @property getScheduledMeetingByChat          [GetScheduledMeetingByChat]
 * @property getChatCall                        [GetChatCall]
 * @property monitorChatCallUpdates             [MonitorChatCallUpdates]
 * @property isConnected True if the app has some network connection, false otherwise.
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val monitorStorageStateEvent: MonitorStorageStateEvent,
    private val startChatCall: StartChatCall,
    private val chatApiGateway: MegaChatApiGateway,
    private val monitorConnectivity: MonitorConnectivity,
    private val answerChatCall: AnswerChatCall,
    private val passcodeManagement: PasscodeManagement,
    private val cameraGateway: CameraGateway,
    private val chatManagement: ChatManagement,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val startChatCallNoRinging: StartChatCallNoRinging,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val getChatCall: GetChatCall,
    private val monitorChatCallUpdates: MonitorChatCallUpdates,
) : ViewModel() {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(ChatState())

    /**
     * public UI State
     */
    val state: StateFlow<ChatState> = _state

    /**
     * Get latest [StorageState] from [MonitorStorageStateEvent] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEvent.getState()

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivity().shareIn(viewModelScope, SharingStarted.Eagerly)

    val isConnected: Boolean
        get() = monitorConnectivity().value

    /**
     * Starts a call.
     *
     * @param chatId The chat id.
     * @param video True, video on. False, video off.
     * @param audio True, audio on. False, video off.
     */
    fun onCallTap(chatId: Long, video: Boolean, audio: Boolean) {
        if (chatApiGateway.getChatCall(chatId) != null) {
            Timber.d("There is a call, open it")
            CallUtil.openMeetingInProgress(
                getInstance().applicationContext,
                chatId,
                true,
                passcodeManagement
            )
            return
        }

        MegaApplication.isWaitingForCall = false

        cameraGateway.setFrontCamera()

        viewModelScope.launch {
            runCatching {
                startChatCall(chatId, video, audio)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { resultStartCall ->
                val resultChatId = resultStartCall.chatHandle
                if (resultChatId != null) {
                    val videoEnable = resultStartCall.flag
                    val paramType = resultStartCall.paramType
                    val audioEnable: Boolean = paramType == ChatRequestParamType.Video

                    CallUtil.addChecksForACall(resultChatId, videoEnable)

                    chatApiGateway.getChatCall(resultChatId)?.let { call ->
                        if (call.isOutgoing) {
                            chatManagement.setRequestSentCall(call.callId, true)
                        }
                    }

                    CallUtil.openMeetingWithAudioOrVideo(
                        getInstance().applicationContext,
                        resultChatId,
                        audioEnable,
                        videoEnable, passcodeManagement
                    )

                }
            }
        }
    }

    /**
     * Set if the chat has been initialised.
     *
     * @param value  True, if the chat has been initialised. False, otherwise.
     */
    fun setChatInitialised(value: Boolean) {
        _state.update { it.copy(isChatInitialised = value) }
    }

    /**
     * Check if the chat has been initialised.
     *
     * @return True, if the chat has been initialised. False, otherwise.
     */
    fun isChatInitialised(): Boolean {
        return state.value.isChatInitialised
    }

    /**
     * Sets chat id
     *
     * @param newChatId   Chat id.
     */
    fun setChatId(newChatId: Long) {
        if (newChatId != chatApiGateway.getChatInvalidHandle() && newChatId != state.value.chatId) {
            _state.update {
                it.copy(
                    chatId = newChatId
                )
            }

            getScheduledMeeting()
        }
    }

    /**
     * Get scheduled meeting
     */
    private fun getScheduledMeeting() =
        viewModelScope.launch {
            runCatching {
                getScheduledMeetingByChat(state.value.chatId)
            }.onFailure {
                Timber.d("Scheduled meeting does not exist")
            }.onSuccess { scheduledMeetingList ->
                scheduledMeetingList?.let { list ->
                    list.forEach { scheduledMeetReceived ->
                        if (scheduledMeetReceived.parentSchedId == chatApiGateway.getChatInvalidHandle()) {
                            _state.update {
                                it.copy(
                                    schedId = scheduledMeetReceived.schedId,
                                    schedIsPending = !scheduledMeetReceived.isPast()
                                )
                            }
                            var scheduledMeetingStatus: ScheduledMeetingStatus? = null
                            getChatCallUpdates()

                            if (!scheduledMeetReceived.isPast()) {
                                Timber.d("Has scheduled meeting")
                                scheduledMeetingStatus = ScheduledMeetingStatus.NotStarted
                                getChatCall(scheduledMeetReceived.chatId)?.let { call ->
                                    when (call.status) {
                                        ChatCallStatus.UserNoPresent -> scheduledMeetingStatus =
                                            ScheduledMeetingStatus.NotJoined
                                        ChatCallStatus.Connecting,
                                        ChatCallStatus.Joining,
                                        ChatCallStatus.InProgress,
                                        -> scheduledMeetingStatus = ScheduledMeetingStatus.Joined
                                        else -> {}
                                    }
                                }
                            }
                            _state.update {
                                it.copy(
                                    scheduledMeetingStatus = scheduledMeetingStatus
                                )
                            }
                        }

                        return@forEach
                    }
                }
            }
        }

    /**
     * Get chat call updates
     */
    private fun getChatCallUpdates() =
        viewModelScope.launch {
            monitorChatCallUpdates()
                .filter { it.chatId == _state.value.chatId }
                .collectLatest { call ->
                    Timber.d("Monitor chat call updated, changes ${call.changes}")
                    if (call.changes == ChatCallChanges.Status && _state.value.schedIsPending) {
                        var scheduledMeetingStatus = ScheduledMeetingStatus.NotStarted
                        when (call.status) {
                            ChatCallStatus.UserNoPresent -> {
                                scheduledMeetingStatus = ScheduledMeetingStatus.NotJoined
                            }
                            ChatCallStatus.Connecting,
                            ChatCallStatus.Joining,
                            ChatCallStatus.InProgress,
                            -> {
                                scheduledMeetingStatus = ScheduledMeetingStatus.Joined
                            }
                            else -> {}
                        }
                        _state.update {
                            it.copy(
                                scheduledMeetingStatus = scheduledMeetingStatus
                            )
                        }
                    }
                }
        }

    /**
     * Start or join scheduled meeting
     */
    fun startOrJoinSchedMeeting() {
        when (_state.value.scheduledMeetingStatus) {
            ScheduledMeetingStatus.NotStarted -> startSchedMeeting()
            ScheduledMeetingStatus.NotJoined -> answerCall(
                _state.value.chatId,
                video = false,
                audio = true
            )
            else -> {}
        }
    }

    /**
     * Start scheduled meeting
     */
    private fun startSchedMeeting() =
        viewModelScope.launch {
            _state.value.schedId?.let { schedId ->
                startChatCallNoRinging(
                    chatId = _state.value.chatId,
                    schedId = schedId,
                    enabledVideo = false,
                    enabledAudio = true
                )?.let { call ->
                    call.chatId.takeIf { it != megaChatApiGateway.getChatInvalidHandle() }
                        ?.let {
                            Timber.d("Meeting started")
                            openCurrentCall(call, true)
                        }
                }
            }
        }

    /**
     * Open current call
     *
     * @param call  [ChatCall]
     * @param isRequestSent True, if it's request sent. False, if not.
     */
    private fun openCurrentCall(call: ChatCall, isRequestSent: Boolean) {
        chatManagement.setSpeakerStatus(call.chatId, call.hasLocalVideo)
        chatManagement.setRequestSentCall(call.callId, isRequestSent)
        passcodeManagement.showPasscodeScreen = true
        getInstance().openCallService(call.chatId)
        _state.update { it.copy(currentCallChatId = call.chatId) }
    }

    /**
     * Remove current chat call
     */
    fun removeCurrentCall() {
        _state.update { it.copy(currentCallChatId = -1L) }
    }

    /**
     * Answer call
     *
     * @param chatId    Chat Id.
     * @param video     True, video on. False, video off
     * @param audio     True, audio on. False, audio off
     */
    private fun answerCall(chatId: Long, video: Boolean, audio: Boolean) {
        cameraGateway.setFrontCamera()
        chatManagement.addJoiningCallChatId(chatId)

        viewModelScope.launch {
            answerChatCall(
                chatId = chatId,
                video = video,
                audio = audio
            )?.let { call ->
                chatManagement.removeJoiningCallChatId(chatId)
                rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                CallUtil.clearIncomingCallNotification(call.callId)
                openCurrentCall(call, false)
                _state.update { it.copy(isCallAnswered = true) }
            }
        }
    }

    /**
     * Answers a call.
     *
     * @param chatId
     * @param video True, video on. False, video off.
     * @param audio True, audio on. False, video off.
     */
    fun onAnswerCall(chatId: Long, video: Boolean, audio: Boolean) {
        if (CallUtil.amIParticipatingInThisMeeting(chatId)) {
            Timber.d("Already participating in this call")
            _state.update { it.copy(isCallAnswered = true) }
            return
        }

        if (MegaApplication.getChatManagement().isAlreadyJoiningCall(chatId)) {
            Timber.d("The call has been answered")
            _state.update { it.copy(isCallAnswered = true) }
            return
        }

        answerCall(chatId, video, audio)
    }
}