package mega.privacy.android.app.presentation.chat

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.main.megachat.ChatActivity
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.chat.model.ChatState
import mega.privacy.android.app.presentation.extensions.getErrorStringId
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.extensions.isPast
import mega.privacy.android.app.usecase.call.EndCallUseCase
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.livedata.SingleLiveEvent
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.PendingMessage
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import mega.privacy.android.domain.entity.contacts.ContactLink
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ScheduledMeetingStatus
import mega.privacy.android.domain.entity.statistics.EndCallEmptyCall
import mega.privacy.android.domain.entity.statistics.EndCallForAll
import mega.privacy.android.domain.entity.statistics.StayOnCallEmptyCall
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.LeaveChat
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.BroadcastChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.LoadPendingMessagesUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.MonitorJoinedSuccessfullyUseCase
import mega.privacy.android.domain.usecase.chat.MonitorLeaveChatUseCase
import mega.privacy.android.domain.usecase.contact.GetContactLinkUseCase
import mega.privacy.android.domain.usecase.contact.IsContactRequestSentUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCall
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdates
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdates
import mega.privacy.android.domain.usecase.meeting.OpenOrStartCall
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.StartChatCall
import mega.privacy.android.domain.usecase.meeting.StartChatCallNoRingingUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import nz.mega.sdk.MegaChatError
import timber.log.Timber
import javax.inject.Inject

/**
 * View Model for [ChatActivity]
 *
 * @property monitorStorageStateEventUseCase                [MonitorStorageStateEventUseCase]
 * @property startChatCall                                  [StartChatCall]
 * @property answerChatCallUseCase                          [AnswerChatCallUseCase]
 * @property passcodeManagement                             [PasscodeManagement]
 * @property setChatVideoInDeviceUseCase                    [SetChatVideoInDeviceUseCase]
 * @property chatManagement                                 [ChatManagement]
 * @property rtcAudioManagerGateway                         [RTCAudioManagerGateway]
 * @property startChatCallNoRingingUseCase                  [StartChatCallNoRingingUseCase]
 * @property startMeetingInWaitingRoomChatUseCase                  [StartMeetingInWaitingRoomChatUseCase]
 * @property getScheduledMeetingByChat                      [GetScheduledMeetingByChat]
 * @property getChatCall                                    [GetChatCall]
 * @property monitorChatCallUpdates                         [MonitorChatCallUpdates]
 * @property endCallUseCase                                 [EndCallUseCase]
 * @property sendStatisticsMeetingsUseCase                  [SendStatisticsMeetingsUseCase]
 * @property isConnected                                    True if the app has some network connection, false otherwise.
 * @property monitorUpdatePushNotificationSettingsUseCase   monitors push notification settings update
 * @property deviceGateway                                  [DeviceGateway]
 * @property getChatRoom                                    [GetChatRoom]
 * @property monitorChatArchivedUseCase                     [MonitorChatArchivedUseCase]
 * @property broadcastChatArchivedUseCase                   [BroadcastChatArchivedUseCase]
 * @property monitorJoinedSuccessfullyUseCase               [MonitorJoinedSuccessfullyUseCase]
 * @property monitorLeaveChatUseCase                        [MonitorLeaveChatUseCase]
 * @property monitorScheduledMeetingUpdates                 [MonitorScheduledMeetingUpdates]
 * @property monitorChatRoomUpdates                         [MonitorChatRoomUpdates]
 * @property leaveChat                                      [LeaveChat]
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val startChatCall: StartChatCall,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val passcodeManagement: PasscodeManagement,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val chatManagement: ChatManagement,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val startChatCallNoRingingUseCase: StartChatCallNoRingingUseCase,
    private val startMeetingInWaitingRoomChatUseCase: StartMeetingInWaitingRoomChatUseCase,
    private val openOrStartCall: OpenOrStartCall,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val getChatCall: GetChatCall,
    private val getChatRoom: GetChatRoom,
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
    private val getContactLinkUseCase: GetContactLinkUseCase,
    private val isContactRequestSentUseCase: IsContactRequestSentUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val loadPendingMessagesUseCase: LoadPendingMessagesUseCase,
    private val monitorScheduledMeetingUpdates: MonitorScheduledMeetingUpdates,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())

    /**
     * UI State Chat
     * Flow of [ChatState]
     */
    val state = _state.asStateFlow()

    /**
     * Check if it's 24 hour format
     */
    val is24HourFormat by lazy { deviceGateway.is24HourFormat() }

    /**
     * Get latest [StorageState] from [MonitorStorageStateEventUseCase] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent = monitorConnectivityUseCase()

    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    private val rxSubscriptions = CompositeDisposable()

    private val newPendingMessage = SingleLiveEvent<PendingMessage>()

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
                if (chatId != INVALID_HANDLE) {
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
     * Check if given feature flag is enabled or not
     */
    fun isFeatureEnabled(feature: Feature) = state.value.enabledFeatureFlags.contains(feature)

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
     * @param shouldCallRing True, calls should ring. False, otherwise.
     */
    fun onCallTap(video: Boolean, shouldCallRing: Boolean) {
        MegaApplication.isWaitingForCall = false
        viewModelScope.launch { setChatVideoInDeviceUseCase() }

        val isWaitingRoom = _state.value.isWaitingRoom
        val isHost = _state.value.isHost
        val hasSchedMeeting = _state.value.scheduledMeeting != null

        when {
            isWaitingRoom -> {
                when {
                    isHost && state.value.scheduledMeetingStatus is ScheduledMeetingStatus.NotJoined -> {
                        answerCall(
                            _state.value.chatId,
                            video = false,
                            audio = true
                        )
                    }

                    isHost && state.value.scheduledMeetingStatus is ScheduledMeetingStatus.NotStarted -> {
                        val schedIdWr: Long =
                            if (!hasSchedMeeting || shouldCallRing) -1L else state.value.scheduledMeeting?.schedId
                                ?: -1L
                        startSchedMeetingWithWaitingRoom(schedIdWr)
                    }

                    !isHost -> {
                        _state.update {
                            it.copy(
                                openWaitingRoomScreen = true
                            )
                        }
                    }
                }

            }

            !hasSchedMeeting -> startCall(
                video = video
            )

            _state.value.scheduledMeetingStatus is ScheduledMeetingStatus.NotStarted ->
                if (shouldCallRing)
                    startCall(video = video)
                else
                    startSchedMeeting()

            _state.value.scheduledMeetingStatus is ScheduledMeetingStatus.NotJoined ->
                answerCall(
                    _state.value.chatId,
                    video = false,
                    audio = true
                )
        }
    }

    /**
     * Sets open waiting room as consumed.
     */
    fun setOpenWaitingRoomConsumed() {
        _state.update { state -> state.copy(openWaitingRoomScreen = false) }
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
        if (newChatId != INVALID_HANDLE && newChatId != state.value.chatId) {
            _state.update {
                it.copy(
                    chatId = newChatId
                )
            }
            getChat()
            getScheduledMeeting()
            getScheduledMeetingUpdates()
        }
    }

    /**
     * Get chat room
     */
    private fun getChat() =
        viewModelScope.launch {
            runCatching {
                getChatRoom(state.value.chatId)
            }.onFailure { exception ->
                Timber.e("Chat room does not exist, finish $exception")
            }.onSuccess { chatRoom ->
                Timber.d("Chat room exists")
                chatRoom?.apply {
                    if (isActive) {
                        Timber.d("Chat room is active")
                        chatRoomUpdated(
                            isWaitingRoom = isWaitingRoom,
                            isHost = ownPrivilege == ChatRoomPermission.Moderator
                        )
                    }
                }
            }
        }

    /**
     * Update waiting room and host values
     */
    fun chatRoomUpdated(
        isWaitingRoom: Boolean = state.value.isWaitingRoom,
        isHost: Boolean = state.value.isHost,
    ) {
        _state.update { state ->
            state.copy(
                isWaitingRoom = isWaitingRoom,
                isHost = isHost
            )
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
                        scheduledMeetingStatus = null,
                        scheduledMeeting = null
                    )
                }
            }.onSuccess { scheduledMeetingList ->
                scheduledMeetingList?.let { list ->
                    list.forEach { scheduledMeetReceived ->
                        if (scheduledMeetReceived.parentSchedId == INVALID_HANDLE) {
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
            }
        }

    /**
     * Get scheduled meeting updates
     */
    private fun getScheduledMeetingUpdates() =
        viewModelScope.launch {
            monitorScheduledMeetingUpdates().collectLatest { scheduledMeetReceived ->
                if (state.value.chatId != scheduledMeetReceived.chatId) {
                    return@collectLatest
                }

                if (scheduledMeetReceived.parentSchedId == INVALID_HANDLE) {
                    return@collectLatest
                }

                scheduledMeetReceived.changes?.let { changes ->
                    Timber.d("Monitor scheduled meeting updated, changes ${scheduledMeetReceived.changes}")
                    changes.forEach {
                        when (it) {
                            ScheduledMeetingChanges.NewScheduledMeeting,
                            ScheduledMeetingChanges.Title,
                            ->
                                _state.update { state ->
                                    state.copy(
                                        schedIsPending = !scheduledMeetReceived.isPast(),
                                        scheduledMeeting = scheduledMeetReceived
                                    )
                                }

                            else -> {}
                        }
                    }
                }
            }
        }

    /**
     * Get scheduled meeting title
     *
     * @return title
     */
    fun getSchedTitle(): String? {
        state.value.scheduledMeeting?.let { schedMeet ->
            schedMeet.title.takeIf { !it.isNullOrEmpty() }?.let {
                return it
            }
        }

        return null
    }

    /**
     * Get chat call updates
     */
    private fun getChatCallUpdates() =
        viewModelScope.launch {
            monitorChatCallUpdates()
                .filter { it.chatId == _state.value.chatId }
                .collectLatest { call ->
                    call.changes?.apply {
                        Timber.d("Monitor chat call updated, changes ${call.changes}")
                        if (contains(ChatCallChanges.Status) && _state.value.schedIsPending) {
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
        }

    /**
     * Start call
     *
     * @param video True, video on. False, video off.
     */
    private fun startCall(video: Boolean) = viewModelScope.launch {
        Timber.d("Start call")
        runCatching {
            openOrStartCall(chatId = _state.value.chatId, video = video, audio = true)
        }.onSuccess { call ->
            call?.apply {
                chatId.takeIf { it != INVALID_HANDLE }?.let {
                    Timber.d("Call started")
                    openCurrentCall(call = this)
                }
            }
        }.onFailure { Timber.w("Exception opening or starting call: $it") }
    }

    /**
     * Start scheduled meeting with waiting room
     *
     * @param schedIdWr   Scheduled meeting id
     */
    private fun startSchedMeetingWithWaitingRoom(schedIdWr: Long) =
        viewModelScope.launch {
            Timber.d("Start scheduled meeting with waiting room schedIdWr")
            runCatching {
                startMeetingInWaitingRoomChatUseCase(
                    chatId = _state.value.chatId,
                    schedIdWr = schedIdWr,
                    enabledVideo = false,
                    enabledAudio = true
                )
            }.onSuccess { call ->
                call?.let {
                    call.chatId.takeIf { it != INVALID_HANDLE }
                        ?.let {
                            Timber.d("Meeting started")
                            openCurrentCall(call = call)
                        }
                }
            }.onFailure {
                Timber.e(it)
            }
        }

    /**
     * Start scheduled meeting
     */
    private fun startSchedMeeting() =
        viewModelScope.launch {
            _state.value.scheduledMeeting?.let { sched ->
                Timber.d("Start scheduled meeting")
                runCatching {
                    startChatCallNoRingingUseCase(
                        chatId = _state.value.chatId,
                        schedId = sched.schedId,
                        enabledVideo = false,
                        enabledAudio = true
                    )
                }.onSuccess { call ->
                    call?.let {
                        call.chatId.takeIf { it != INVALID_HANDLE }
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
                currentCallChatId = INVALID_HANDLE,
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
        chatManagement.addJoiningCallChatId(chatId)

        viewModelScope.launch {
            Timber.d("Answer call")
            runCatching {
                setChatVideoInDeviceUseCase()
                answerChatCallUseCase(chatId = chatId, video = video, audio = audio)
            }.onSuccess { call ->
                call?.apply {
                    chatManagement.removeJoiningCallChatId(chatId)
                    rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                    CallUtil.clearIncomingCallNotification(callId)
                    openCurrentCall(this)
                    _state.update { it.copy(isCallAnswered = true) }
                }
            }.onFailure { error ->
                if (error is MegaException && error.errorCode == MegaChatError.ERROR_ACCESS) {
                    _state.update {
                        it.copy(
                            isWaitingRoom = true,
                            openWaitingRoomScreen = true
                        )
                    }
                } else {
                    Timber.w("Exception answering call: $error")
                }
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
        viewModelScope.launch {
            runCatching {
                getChatCall(state.value.chatId)
            }.onFailure { exception ->
                Timber.e("Call does not exist $exception")
            }.onSuccess { call ->
                Timber.d("Call exists")
                call?.apply {
                    endCallUseCase.hangCall(call.callId)
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribeBy(onError = { error ->
                            Timber.e(error.stackTraceToString())
                        })
                        .addTo(rxSubscriptions)
                }
            }
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
     * Check if it's a pending scheduled meeting
     *
     * @return  True, if it's pending scheduled meeting. False, if not.
     */
    fun isPendingMeeting() =
        _state.value.schedIsPending

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

    /**
     * Get contact link by handle
     */
    fun getContactLinkByHandle(
        userHandle: Long,
        onLoadContactLink: (contactLink: ContactLink) -> Unit,
    ) {
        viewModelScope.launch {
            runCatching {
                getContactLinkUseCase(userHandle)
            }.onSuccess { contactLink ->
                onLoadContactLink(contactLink)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Check contact request sent
     *
     * @param email
     * @param name
     */
    fun checkContactRequestSent(email: String, name: String) {
        viewModelScope.launch {
            runCatching {
                isContactRequestSentUseCase(email)
            }.onSuccess { isSent ->
                _state.update {
                    it.copy(
                        contactInvitation = ContactInvitation(
                            isSent = isSent,
                            email = email,
                            name = name
                        )
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * On contact invitation consumed
     */
    fun onContactInvitationConsumed() {
        _state.update { it.copy(contactInvitation = null) }
    }

    /**
     * Load pending messages.
     *
     */
    fun loadPendingMessages() = viewModelScope.launch {
        loadPendingMessagesUseCase(state.value.chatId)
            .collect { pendingMessage -> newPendingMessage.value = pendingMessage }
    }


    /**
     * On pending message loaded
     *
     * @return
     */
    fun onPendingMessageLoaded(): LiveData<PendingMessage> = newPendingMessage

    companion object {
        private const val INVALID_HANDLE = -1L
    }

}
