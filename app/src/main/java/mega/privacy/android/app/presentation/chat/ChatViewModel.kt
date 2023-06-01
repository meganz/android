package mega.privacy.android.app.presentation.chat

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.gateway.CameraGateway
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.chat.model.ChatState
import mega.privacy.android.app.presentation.extensions.getErrorStringId
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.extensions.isPast
import mega.privacy.android.app.usecase.call.EndCallUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus
import mega.privacy.android.domain.entity.statistics.EndCallEmptyCall
import mega.privacy.android.domain.entity.statistics.EndCallForAll
import mega.privacy.android.domain.entity.statistics.StayOnCallEmptyCall
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.LeaveChat
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.BroadcastChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.MonitorJoinedSuccessfullyUseCase
import mega.privacy.android.domain.usecase.chat.MonitorLeaveChatUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCall
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import mega.privacy.android.domain.usecase.meeting.OpenOrStartCall
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.StartChatCall
import mega.privacy.android.domain.usecase.meeting.StartChatCallNoRingingUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import nz.mega.sdk.MegaChatApiJava
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [mega.privacy.android.app.main.megachat.ChatActivity]
 *
 * @property monitorStorageStateEventUseCase                [MonitorStorageStateEventUseCase]
 * @property startChatCall                                  [StartChatCall]
 * @property chatApiGateway                                 [MegaChatApiGateway]
 * @property answerChatCallUseCase                          [AnswerChatCallUseCase]
 * @property passcodeManagement                             [PasscodeManagement]
 * @property cameraGateway                                  [CameraGateway]
 * @property chatManagement                                 [ChatManagement]
 * @property rtcAudioManagerGateway                         [RTCAudioManagerGateway]
 * @property startChatCallNoRingingUseCase                  [StartChatCallNoRingingUseCase]
 * @property megaChatApiGateway                             [MegaChatApiGateway]
 * @property getScheduledMeetingByChat                      [GetScheduledMeetingByChat]
 * @property getChatCall                                    [GetChatCall]
 * @property monitorChatCallUpdates                         [MonitorChatCallUpdates]
 * @property endCallUseCase                                 [EndCallUseCase]
 * @property sendStatisticsMeetingsUseCase                  [SendStatisticsMeetingsUseCase]
 * @property isConnected                                    True if the app has some network connection, false otherwise.
 * @property monitorUpdatePushNotificationSettingsUseCase   monitors push notification settings update
 * @property deviceGateway                                  [DeviceGateway]
 * @property monitorChatArchivedUseCase                     [MonitorChatArchivedUseCase]
 * @property broadcastChatArchivedUseCase                   [BroadcastChatArchivedUseCase]
 * @property monitorJoinedSuccessfullyUseCase               [MonitorJoinedSuccessfullyUseCase]
 * @property monitorLeaveChatUseCase                        [MonitorLeaveChatUseCase]
 * @property leaveChat                                      [LeaveChat]
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val startChatCall: StartChatCall,
    private val chatApiGateway: MegaChatApiGateway,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val passcodeManagement: PasscodeManagement,
    private val cameraGateway: CameraGateway,
    private val chatManagement: ChatManagement,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val startChatCallNoRingingUseCase: StartChatCallNoRingingUseCase,
    private val openOrStartCall: OpenOrStartCall,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val getChatCall: GetChatCall,
    private val monitorChatCallUpdates: MonitorChatCallUpdates,
    private val endCallUseCase: EndCallUseCase,
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val deviceGateway: DeviceGateway,
    private val monitorChatArchivedUseCase: MonitorChatArchivedUseCase,
    private val broadcastChatArchivedUseCase: BroadcastChatArchivedUseCase,
    private val monitorJoinedSuccessfullyUseCase: MonitorJoinedSuccessfullyUseCase,
    private val monitorLeaveChatUseCase: MonitorLeaveChatUseCase,
    private val leaveChat: LeaveChat,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())

    /**
     * UI State Chat
     * Flow of [ChatState]
     */
    val state = _state.asStateFlow()

    val is24HourFormat by lazy { deviceGateway.is24HourFormat() }

    /**
     * Get latest [StorageState] from [MonitorStorageStateEventUseCase] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivityUseCase().shareIn(viewModelScope, SharingStarted.Eagerly)

    val isConnected: Boolean
        get() = monitorConnectivityUseCase().value

    private val rxSubscriptions = CompositeDisposable()

    init {
        viewModelScope.launch {
            monitorUpdatePushNotificationSettingsUseCase().collect {
                _state.update { it.copy(isPushNotificationSettingsUpdatedEvent = true) }
            }
        }

        viewModelScope.launch {
            monitorChatArchivedUseCase().conflate().collect { chatTitle ->
                _state.update { it.copy(titleChatArchivedEvent = chatTitle) }
            }
        }

        viewModelScope.launch {
            monitorJoinedSuccessfullyUseCase().conflate().collect {
                _state.update { it.copy(isJoiningOrLeaving = false) }
            }
        }

        viewModelScope.launch {
            monitorLeaveChatUseCase().conflate().collect { chatId ->
                if (chatId != MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                    if (state.value.chatId == chatId) {
                        _state.update { state ->
                            state.copy(
                                isJoiningOrLeaving = true,
                                joiningOrLeavingAction = R.string.leaving_label
                            )
                        }
                    }
                    performLeaveChat(chatId)
                }
            }
        }
    }

    override fun onCleared() {
        rxSubscriptions.clear()
        super.onCleared()
    }

    /**
     * Leave a chat
     *
     * @param chatId    [Long] ID of the chat to leave.
     */
    private fun performLeaveChat(chatId: Long) = viewModelScope.launch {
        runCatching { leaveChat(chatId) }
            .onSuccess { setIsJoiningOrLeaving(false, null) }
            .onFailure {
                if (it is MegaException) {
                    _state.update { state -> state.copy(snackbarMessage = it.getErrorStringId()) }
                }
            }
    }

    /**
     * Sets snackbarMessage in state as consumed.
     */
    fun onSnackbarMessageConsumed() =
        _state.update { state -> state.copy(snackbarMessage = null) }

    /**
     * Call button clicked
     *
     * @param video True, video on. False, video off.
     */
    fun onCallTap(video: Boolean) {
        MegaApplication.isWaitingForCall = false
        cameraGateway.setFrontCamera()

        when {
            _state.value.schedId == null || _state.value.schedId == megaChatApiGateway.getChatInvalidHandle() -> startCall(
                video = video
            )
            _state.value.scheduledMeetingStatus is ScheduledMeetingStatus.NotStarted -> startSchedMeeting()
            _state.value.scheduledMeetingStatus is ScheduledMeetingStatus.NotJoined -> answerCall(
                _state.value.chatId,
                video = false,
                audio = true
            )
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
    fun isChatInitialised(): Boolean = state.value.isChatInitialised

    /**
     * Check if joining or leaving the chat.
     *
     * @return True, if user is joining or leaving the chat. False, otherwise.
     */
    fun isJoiningOrLeaving(): Boolean = _state.value.isJoiningOrLeaving

    /**
     * Set if the user is joining or leaving the chat.
     *
     * @param value True, if user is joining or leaving the chat. False, otherwise.
     */
    fun setIsJoiningOrLeaving(value: Boolean, @StringRes actionId: Int?) {
        _state.update { it.copy(isJoiningOrLeaving = value, joiningOrLeavingAction = actionId) }
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
                _state.update {
                    it.copy(
                        schedId = megaChatApiGateway.getChatInvalidHandle(),
                        scheduledMeetingStatus = null
                    )
                }
            }.onSuccess { scheduledMeetingList ->
                scheduledMeetingList?.let { list ->
                    list.forEach { scheduledMeetReceived ->
                        if (scheduledMeetReceived.parentSchedId == chatApiGateway.getChatInvalidHandle()) {
                            var scheduledMeetingStatus: ScheduledMeetingStatus =
                                ScheduledMeetingStatus.NotStarted
                            if (!scheduledMeetReceived.isPast()) {
                                Timber.d("Has scheduled meeting")
                                getChatCall(scheduledMeetReceived.chatId)?.let { call ->
                                    scheduledMeetingStatus = when (call.status) {
                                        ChatCallStatus.UserNoPresent ->
                                            ScheduledMeetingStatus.NotJoined(call.duration)
                                        ChatCallStatus.Connecting,
                                        ChatCallStatus.Joining,
                                        ChatCallStatus.InProgress,
                                        -> ScheduledMeetingStatus.Joined(call.duration)
                                        else -> ScheduledMeetingStatus.NotStarted
                                    }
                                }
                            }

                            _state.update {
                                it.copy(
                                    schedId = scheduledMeetReceived.schedId,
                                    schedIsPending = !scheduledMeetReceived.isPast(),
                                    scheduledMeetingStatus = scheduledMeetingStatus,
                                    scheduledMeeting = scheduledMeetReceived
                                )
                            }
                            return@forEach
                        }
                    }
                }

                getChatCallUpdates()

                if (_state.value.schedId == null) {
                    _state.update {
                        it.copy(
                            schedId = megaChatApiGateway.getChatInvalidHandle(),
                            scheduledMeetingStatus = null
                        )
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
                        val scheduledMeetingStatus = when (call.status) {
                            ChatCallStatus.UserNoPresent ->
                                ScheduledMeetingStatus.NotJoined(call.duration)
                            ChatCallStatus.Connecting,
                            ChatCallStatus.Joining,
                            ChatCallStatus.InProgress,
                            -> ScheduledMeetingStatus.Joined(call.duration)
                            else -> ScheduledMeetingStatus.NotStarted
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
     * Start call
     *
     * @param video True, video on. False, video off.
     */
    private fun startCall(video: Boolean) = viewModelScope.launch {
        Timber.d("Start call")
        openOrStartCall(
            chatId = _state.value.chatId, video = video, audio = true
        )?.let { call ->
            call.chatId.takeIf { it != megaChatApiGateway.getChatInvalidHandle() }?.let {
                Timber.d("Call started")
                openCurrentCall(call = call)
            }
        }
    }

    /**
     * Start scheduled meeting
     */
    private fun startSchedMeeting() =
        viewModelScope.launch {
            _state.value.schedId?.let { schedId ->
                if (schedId != megaChatApiGateway.getChatInvalidHandle()) {
                    Timber.d("Start scheduled meeting")
                    runCatching {
                        startChatCallNoRingingUseCase(
                            chatId = _state.value.chatId,
                            schedId = schedId,
                            enabledVideo = false,
                            enabledAudio = true
                        )
                    }.onSuccess { call ->
                        call?.let {
                            call.chatId.takeIf { it != megaChatApiGateway.getChatInvalidHandle() }
                                ?.let {
                                    Timber.d("Meeting started")
                                    openCurrentCall(call = call)
                                }
                        }
                    }.onFailure {
                        Timber.e(it)
                    }
                }
            }
        }

    /**
     * Open current call
     *
     * @param call  [ChatCall]
     */
    private fun openCurrentCall(call: ChatCall) {
        chatManagement.setSpeakerStatus(call.chatId, call.hasLocalVideo)
        chatManagement.setRequestSentCall(call.callId, call.isOutgoing)
        passcodeManagement.showPasscodeScreen = true
        getInstance().openCallService(call.chatId)
        _state.update {
            it.copy(
                currentCallChatId = call.chatId,
                currentCallAudioStatus = call.hasLocalAudio,
                currentCallVideoStatus = call.hasLocalVideo
            )
        }
    }

    /**
     * Remove current chat call
     */
    fun removeCurrentCall() {
        _state.update {
            it.copy(
                currentCallChatId = megaChatApiGateway.getChatInvalidHandle(),
                currentCallVideoStatus = false,
                currentCallAudioStatus = false
            )
        }
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
            Timber.d("Answer call")
            answerChatCallUseCase(
                chatId = chatId,
                video = video,
                audio = audio
            )?.let { call ->
                chatManagement.removeJoiningCallChatId(chatId)
                rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                CallUtil.clearIncomingCallNotification(call.callId)
                openCurrentCall(call)
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

    /**
     * Control when Stay on call option is chosen
     */
    fun checkStayOnCall() {
        MegaApplication.getChatManagement().stopCounterToFinishCall()
        MegaApplication.getChatManagement().hasEndCallDialogBeenIgnored = true

        viewModelScope.launch {
            kotlin.runCatching {
                sendStatisticsMeetingsUseCase(StayOnCallEmptyCall())
            }
        }
    }

    /**
     * Control when End call now option is chosen
     */
    fun checkEndCall() {
        MegaApplication.getChatManagement().stopCounterToFinishCall()

        chatApiGateway.getChatCall(_state.value.chatId)?.let { call ->
            endCallUseCase.hangCall(call.callId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onError = { error ->
                    Timber.e(error.stackTraceToString())
                })
                .addTo(rxSubscriptions)
        }

        viewModelScope.launch {
            kotlin.runCatching {
                sendStatisticsMeetingsUseCase(EndCallEmptyCall())
            }
        }
    }

    /**
     * End for all the current call
     */
    fun endCallForAll() {
        endCallUseCase.run {
            endCallForAllWithChatId(_state.value.chatId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onError = { error ->
                    Timber.e(error.stackTraceToString())
                })
                .addTo(rxSubscriptions)
        }

        viewModelScope.launch {
            kotlin.runCatching {
                sendStatisticsMeetingsUseCase(EndCallForAll())
            }
        }
    }

    /**
     * on Consume Push notification settings updated event
     */
    fun onConsumePushNotificationSettingsUpdateEvent() {
        viewModelScope.launch {
            _state.update { it.copy(isPushNotificationSettingsUpdatedEvent = false) }
        }
    }

    /**
     * Get scheduled meeting
     *
     * @return  [ChatScheduledMeeting]
     */
    fun getMeeting(): ChatScheduledMeeting? =
        _state.value.scheduledMeeting

    /**
     * Launch broadcast for a chat archived event
     *
     * @param chatTitle [String]
     */
    fun launchBroadcastChatArchived(chatTitle: String) = viewModelScope.launch {
        broadcastChatArchivedUseCase(chatTitle)
    }

    /**
     * Consume chat archive event
     */
    fun onChatArchivedEventConsumed() =
        _state.update { it.copy(titleChatArchivedEvent = null) }
}