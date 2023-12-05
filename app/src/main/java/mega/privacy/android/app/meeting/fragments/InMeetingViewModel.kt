package mega.privacy.android.app.meeting.fragments

import android.content.Context
import android.graphics.Bitmap
import android.view.TextureView
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.constants.EventConstants.EVENT_UPDATE_CALL
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.listeners.EditChatRoomNameListener
import mega.privacy.android.app.listeners.GetUserEmailListener
import mega.privacy.android.app.main.listeners.CreateGroupChatWithPublicLink
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.meeting.listeners.GroupVideoListener
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.meeting.model.InMeetingUiState
import mega.privacy.android.app.usecase.call.EndCallUseCase
import mega.privacy.android.app.usecase.call.GetCallStatusChangesUseCase
import mega.privacy.android.app.usecase.call.GetCallUseCase
import mega.privacy.android.app.usecase.call.GetNetworkChangesUseCase
import mega.privacy.android.app.usecase.call.GetParticipantsChangesUseCase
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.getTitleChat
import mega.privacy.android.app.utils.Constants.AVATAR_CHANGE
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.NAME_CHANGE
import mega.privacy.android.app.utils.Constants.TYPE_JOIN
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.meeting.AnotherCallType
import mega.privacy.android.domain.entity.meeting.CallUIStatusType
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.SubtitleCallType
import mega.privacy.android.domain.entity.statistics.EndCallEmptyCall
import mega.privacy.android.domain.entity.statistics.EndCallForAll
import mega.privacy.android.domain.entity.statistics.StayOnCallEmptyCall
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.meeting.BroadcastCallEndedUseCase
import mega.privacy.android.domain.usecase.meeting.EnableAudioLevelMonitorUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.IsAudioLevelMonitorEnabledUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase

import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.RequestHighResolutionVideoUseCase
import mega.privacy.android.domain.usecase.meeting.RequestLowResolutionVideoUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.StartChatCall
import mega.privacy.android.domain.usecase.meeting.StopHighResolutionVideoUseCase
import mega.privacy.android.domain.usecase.meeting.StopLowResolutionVideoUseCase
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatCall
import nz.mega.sdk.MegaChatCall.CALL_STATUS_CONNECTING
import nz.mega.sdk.MegaChatCall.CALL_STATUS_IN_PROGRESS
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatRoom.PRIV_MODERATOR
import nz.mega.sdk.MegaChatSession
import nz.mega.sdk.MegaChatVideoListenerInterface
import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaRequestListenerInterface
import org.jetbrains.anko.defaultSharedPreferences
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * InMeetingFragment view model.
 *
 * @property inMeetingRepository                [InMeetingRepository]
 * @property getCallUseCase                     [GetCallUseCase]
 * @property startChatCall                      [StartChatCall]
 * @property getNetworkChangesUseCase           [GetNetworkChangesUseCase]
 * @property getCallStatusChangesUseCase        [GetCallStatusChangesUseCase]
 * @property endCallUseCase                     [EndCallUseCase]
 * @property getParticipantsChangesUseCase      [GetParticipantsChangesUseCase]
 * @property rtcAudioManagerGateway             [RTCAudioManagerGateway]
 * @property setChatVideoInDeviceUseCase        [SetChatVideoInDeviceUseCase]
 * @property megaChatApiGateway                 [MegaChatApiGateway]
 * @property passcodeManagement                 [PasscodeManagement]
 * @property chatManagement                     [ChatManagement]
 * @property sendStatisticsMeetingsUseCase      [SendStatisticsMeetingsUseCase]
 * @property enableAudioLevelMonitorUseCase     [EnableAudioLevelMonitorUseCase]
 * @property isAudioLevelMonitorEnabledUseCase  [IsAudioLevelMonitorEnabledUseCase]
 * @property monitorChatSessionUpdatesUseCase   [MonitorChatSessionUpdatesUseCase]
 * @property requestHighResolutionVideoUseCase  [RequestHighResolutionVideoUseCase]
 * @property requestLowResolutionVideoUseCase   [RequestLowResolutionVideoUseCase]
 * @property stopHighResolutionVideoUseCase     [StopHighResolutionVideoUseCase]
 * @property stopLowResolutionVideoUseCase      [StopLowResolutionVideoUseCase]
 * @property getChatCallUseCase                 [GetChatCallUseCase]
 * @property getChatRoomUseCase                 [GetChatRoom]
 * @property monitorChatRoomUpdates             [MonitorChatRoomUpdates]
 * @property state                              Current view state as [InMeetingUiState]
 */
@HiltViewModel
class InMeetingViewModel @Inject constructor(
    private val inMeetingRepository: InMeetingRepository,
    private val getCallUseCase: GetCallUseCase,
    private val startChatCall: StartChatCall,
    private val getNetworkChangesUseCase: GetNetworkChangesUseCase,
    private val getCallStatusChangesUseCase: GetCallStatusChangesUseCase,
    private val endCallUseCase: EndCallUseCase,
    private val getParticipantsChangesUseCase: GetParticipantsChangesUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val passcodeManagement: PasscodeManagement,
    private val chatManagement: ChatManagement,
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase,
    private val enableAudioLevelMonitorUseCase: EnableAudioLevelMonitorUseCase,
    private val isAudioLevelMonitorEnabledUseCase: IsAudioLevelMonitorEnabledUseCase,
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val requestHighResolutionVideoUseCase: RequestHighResolutionVideoUseCase,
    private val requestLowResolutionVideoUseCase: RequestLowResolutionVideoUseCase,
    private val stopHighResolutionVideoUseCase: StopHighResolutionVideoUseCase,
    private val stopLowResolutionVideoUseCase: StopLowResolutionVideoUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val getChatRoomUseCase: GetChatRoom,
    private val broadcastCallEndedUseCase: BroadcastCallEndedUseCase,
) : BaseRxViewModel(), EditChatRoomNameListener.OnEditedChatRoomNameCallback,
    GetUserEmailListener.OnUserEmailUpdateCallback {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(InMeetingUiState())

    /**
     * public UI State
     */
    val state: StateFlow<InMeetingUiState> = _state

    private var anotherCallInProgressDisposable: Disposable? = null
    private var networkQualityDisposable: Disposable? = null
    private var reconnectingDisposable: Disposable? = null

    private val _pinItemEvent = MutableLiveData<Event<Participant>>()
    val pinItemEvent: LiveData<Event<Participant>> = _pinItemEvent


    /**
     * Participant selected
     *
     * @param peerId Peer Id
     * @param clientId  Client Id
     */
    fun onItemClick(peerId: Long, clientId: Long) {
        onSnackbarMessageConsumed()
        getParticipant(peerId, clientId)?.let { participant ->
            getSession(clientId)?.let {
                if (it.hasScreenShare() && _state.value.callUIStatus == CallUIStatusType.SpeakerView) {
                    _state.update { state ->
                        state.copy(
                            snackbarMessage = triggered(R.string.meetings_meeting_screen_main_view_participant_is_sharing_screen_warning),
                        )
                    }
                }
            }
            _pinItemEvent.value = Event(participant)
        }
    }

    /**
     * Chat participant select to be in speaker view
     *
     * @param chatParticipant [ChatParticipant]
     */
    fun onItemClick(chatParticipant: ChatParticipant) =
        participants.value?.find { it.peerId == chatParticipant.handle }?.let {
            onItemClick(peerId = it.peerId, clientId = it.clientId)
        }

    // Meeting
    private val _callLiveData = MutableLiveData<MegaChatCall?>(null)
    val callLiveData: LiveData<MegaChatCall?> = _callLiveData

    // Call ID
    private val _updateCallId = MutableStateFlow(MEGACHAT_INVALID_HANDLE)
    val updateCallId: StateFlow<Long> get() = _updateCallId

    private val _showPoorConnectionBanner = MutableStateFlow(false)
    val showPoorConnectionBanner: StateFlow<Boolean> get() = _showPoorConnectionBanner

    private val _showReconnectingBanner = MutableStateFlow(false)
    val showReconnectingBanner: StateFlow<Boolean> get() = _showReconnectingBanner

    private val _showOnlyMeBanner = MutableStateFlow(false)
    val showOnlyMeBanner: StateFlow<Boolean> get() = _showOnlyMeBanner

    private val _showWaitingForOthersBanner = MutableStateFlow(false)
    val showWaitingForOthersBanner: StateFlow<Boolean> get() = _showWaitingForOthersBanner

    private val _showEndMeetingAsModeratorBottomPanel = MutableLiveData<Boolean>()
    val showEndMeetingAsModeratorBottomPanel: LiveData<Boolean> =
        _showEndMeetingAsModeratorBottomPanel

    private val _showAssignModeratorBottomPanel = MutableLiveData<Boolean>()
    val showAssignModeratorBottomPanel: LiveData<Boolean> = _showAssignModeratorBottomPanel


    // List of participants in the meeting
    val participants: MutableLiveData<MutableList<Participant>> = MutableLiveData(mutableListOf())

    // List of speaker participants in the meeting
    val speakerParticipants: MutableLiveData<MutableList<Participant>> =
        MutableLiveData(mutableListOf())

    // List of visible participants in the meeting
    var visibleParticipants: MutableList<Participant> = mutableListOf()

    private val _getParticipantsChanges =
        MutableStateFlow<Pair<Int, ((Context) -> String)?>>(Pair(TYPE_JOIN, null))
    val getParticipantsChanges: StateFlow<Pair<Int, ((Context) -> String)?>> get() = _getParticipantsChanges

    private val updateCallObserver =
        Observer<MegaChatCall> {
            if (isSameChatRoom(it.chatid)) {
                _callLiveData.value = it
            }
        }

    private val noOutgoingCallObserver = Observer<Long> {
        _state.value.call?.apply {
            if (it == chatId) {
                status?.let {
                    checkSubtitleToolbar()
                }
            }
        }
    }

    private val waitingForOthersBannerObserver =
        Observer<Pair<Long, Boolean>> { result ->
            val chatId: Long = result.first
            val onlyMeInTheCall: Boolean = result.second
            if (_state.value.currentChatId == chatId) {
                if (onlyMeInTheCall) {
                    _showWaitingForOthersBanner.value = false
                    if (!MegaApplication.getChatManagement().hasEndCallDialogBeenIgnored) {
                        _showOnlyMeBanner.value = true
                    }
                }
            }
        }

    init {
        startMonitorChatRoomUpdates()
        startMonitorChatCallUpdates()
        startMonitorChatSessionUpdates()

        getParticipantsChangesUseCase.getChangesFromParticipants()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { (chatId, typeChange, peers) ->
                    if (_state.value.currentChatId == chatId) {
                        getChat()?.let { chat ->
                            if (chat.isMeeting || chat.isGroup) {
                                peers?.let { list ->
                                    getParticipantChanges(list, typeChange)
                                }
                            }
                        }
                    }
                },
                onError = Timber::e
            )
            .addTo(composite)

        getParticipantsChangesUseCase.checkIfIAmAloneOnAnyCall()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { (chatId, onlyMeInTheCall, waitingForOthers, isReceivedChange) ->
                    if (_state.value.currentChatId == chatId) {
                        val millisecondsOnlyMeInCallDialog =
                            TimeUnit.MILLISECONDS.toSeconds(MegaApplication.getChatManagement().millisecondsOnlyMeInCallDialog)

                        if (onlyMeInTheCall) {
                            hideBottomPanels()
                            if (waitingForOthers && millisecondsOnlyMeInCallDialog <= 0) {
                                _showOnlyMeBanner.value = false
                                _showWaitingForOthersBanner.value = true
                            } else {
                                _showWaitingForOthersBanner.value = false
                                if (waitingForOthers || !isReceivedChange) {
                                    _showOnlyMeBanner.value = true
                                }
                            }
                        } else {
                            _showWaitingForOthersBanner.value = false
                            _showOnlyMeBanner.value = false
                        }
                    }
                },
                onError = Timber::e
            )
            .addTo(composite)

        LiveEventBus.get(EVENT_UPDATE_CALL, MegaChatCall::class.java)
            .observeForever(updateCallObserver)

        LiveEventBus.get(EventConstants.EVENT_NOT_OUTGOING_CALL, Long::class.java)
            .observeForever(noOutgoingCallObserver)

        LiveEventBus.get<Pair<Long, Boolean>>(EventConstants.EVENT_UPDATE_WAITING_FOR_OTHERS)
            .observeForever(waitingForOthersBannerObserver)
    }

    /**
     * Get chat room
     */
    private fun getChatRoom() {
        viewModelScope.launch {
            runCatching {
                getChatRoomUseCase(_state.value.currentChatId)
            }.onSuccess { chatRoom ->
                chatRoom?.let { chat ->
                    _state.update { state ->
                        state.copy(
                            chatTitle = chat.title,
                            isOpenInvite = chat.isOpenInvite,
                            isOneToOneCall = !chat.isGroup && !chat.isMeeting,
                            isMeeting = chat.isMeeting,
                            isPublicChat = chat.isPublic
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
    private fun getChatCall(context: Context) {
        viewModelScope.launch {
            runCatching {
                getChatCallUseCase(_state.value.currentChatId)
            }.onSuccess { chatCall ->
                chatCall?.let { call ->
                    _state.update { it.copy(call = call) }
                    call.status?.let { status ->
                        checkSubtitleToolbar()
                        setCall(_state.value.currentChatId, context)
                        if (status != ChatCallStatus.Initial && _state.value.previousState == ChatCallStatus.Initial) {
                            _state.update {
                                it.copy(
                                    previousState = status,
                                )
                            }
                        }
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Get chat room updates
     */
    private fun startMonitorChatRoomUpdates() =
        viewModelScope.launch {
            monitorChatRoomUpdates(_state.value.currentChatId).collectLatest { chat ->
                if (chat.hasChanged(ChatRoomChange.Title)) {
                    Timber.d("Changes in chat title")
                    _state.update { state ->
                        state.copy(
                            chatTitle = chat.title,
                        )
                    }
                }
                if (chat.hasChanged(ChatRoomChange.OpenInvite)) {
                    _state.update {
                        it.copy(isOpenInvite = chat.isOpenInvite)
                    }
                }
            }
        }

    /**
     * Get chat session updates
     */
    private fun startMonitorChatSessionUpdates() =
        viewModelScope.launch {
            monitorChatSessionUpdatesUseCase()
                .filter { it.chatId == _state.value.currentChatId }
                .collectLatest { result ->
                    getChatCallUseCase(result.chatId)?.let { call ->
                        _state.update { it.copy(call = call) }
                    }
                    checkSubtitleToolbar()
                }
        }

    /**
     * Get chat call updates
     */
    private fun startMonitorChatCallUpdates() =
        viewModelScope.launch {
            monitorChatCallUpdatesUseCase()
                .filter { it.chatId == _state.value.currentChatId }
                .collectLatest { call ->
                    _state.update { it.copy(call = call) }

                    call.changes?.apply {
                        if (contains(ChatCallChanges.Status)) {
                            call.status?.let { status ->
                                checkSubtitleToolbar()
                                _state.update { state ->
                                    state.copy(
                                        previousState = status,
                                    )
                                }
                            }
                        }
                    }
                }
        }

    /**
     * Show meeting info dialog
     *
     * @param shouldShowDialog True,show dialog.
     */
    fun onToolbarTap(shouldShowDialog: Boolean) =
        _state.update {
            it.copy(showMeetingInfoFragment = !it.isOneToOneCall && shouldShowDialog)
        }

    /**
     * Method to check if only me dialog and the call will end banner should be displayed.
     */
    fun checkShowOnlyMeBanner() {
        if (isOneToOneCall())
            return

        _callLiveData.value?.let { call ->
            getParticipantsChangesUseCase.checkIfIAmAloneOnSpecificCall(call).let { result ->
                if (result.onlyMeInTheCall) {
                    if (_showOnlyMeBanner.value) {
                        _showOnlyMeBanner.value = false
                    }

                    _showWaitingForOthersBanner.value = false
                    _showOnlyMeBanner.value = true
                }
            }
        }
    }

    /**
     * Method to get right text to display on the banner
     *
     * @param list List of participants with changes
     * @param type Type of change
     */
    private fun getParticipantChanges(list: ArrayList<Long>, type: Int) {
        val action = when (val numParticipants = list.size) {
            1 -> { context: Context ->
                context.getString(
                    if (type == TYPE_JOIN)
                        R.string.meeting_call_screen_one_participant_joined_call
                    else
                        R.string.meeting_call_screen_one_participant_left_call,
                    getParticipantFullName(list[0])
                )
            }

            2 -> { context: Context ->
                context.getString(
                    if (type == TYPE_JOIN)
                        R.string.meeting_call_screen_two_participants_joined_call
                    else
                        R.string.meeting_call_screen_two_participants_left_call,
                    getParticipantFullName(list[0]), getParticipantFullName(list[1])
                )
            }

            else -> { context: Context ->
                context.resources.getQuantityString(
                    if (type == TYPE_JOIN) R.plurals.meeting_call_screen_more_than_two_participants_joined_call
                    else
                        R.plurals.meeting_call_screen_more_than_two_participants_left_call,
                    numParticipants,
                    getParticipantFullName(list[0]),
                    (numParticipants - 1)
                )
            }
        }

        _getParticipantsChanges.value = Pair(type, action)
    }

    /**
     * Method to check the subtitle in the toolbar
     */
    private fun checkSubtitleToolbar() =
        _state.value.call?.apply {
            when (status) {
                ChatCallStatus.Connecting -> _state.update { state ->
                    state.copy(
                        showCallDuration = false,
                        updateCallSubtitle = SubtitleCallType.Connecting
                    )
                }

                ChatCallStatus.InProgress -> {
                    val isCalling = !_state.value.isMeeting && isRequestSent() && this.isOutgoing
                    _state.update { state ->
                        state.copy(
                            showCallDuration = !isCalling,
                            updateCallSubtitle = if (isCalling) SubtitleCallType.Calling else SubtitleCallType.Established
                        )
                    }
                }

                else -> {}
            }
        }

    /**
     * Method to get the duration of the call
     */
    fun getCallDuration(): Long = getCall()?.duration ?: INVALID_VALUE.toLong()

    /**
     * Method that controls whether the another call banner should be visible or not
     */
    private fun checkAnotherCallBanner() {
        anotherCallInProgressDisposable?.dispose()

        anotherCallInProgressDisposable =
            getCallUseCase.checkAnotherCall(_state.value.currentChatId).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread()).subscribeBy(
                    onNext = {
                        if (it == MEGACHAT_INVALID_HANDLE) {
                            _state.update { state ->
                                state.copy(
                                    updateAnotherCallBannerType = AnotherCallType.NotCall,
                                )
                            }
                        } else {
                            val call: MegaChatCall =
                                getCallUseCase.getMegaChatCall(it).blockingGet()
                            if (call.isOnHold && _state.value.updateAnotherCallBannerType != AnotherCallType.CallOnHold) {
                                _state.update { state ->
                                    state.copy(
                                        updateAnotherCallBannerType = AnotherCallType.CallOnHold,
                                    )
                                }

                            } else if (!call.isOnHold && _state.value.updateAnotherCallBannerType != AnotherCallType.CallInProgress) {
                                _state.update { state ->
                                    state.copy(
                                        updateAnotherCallBannerType = AnotherCallType.CallInProgress,
                                    )
                                }
                            }

                            inMeetingRepository.getChatRoom(it)?.let { chat ->
                                _state.update { state ->
                                    state.copy(
                                        anotherChatTitle = getTitleChat(chat),
                                    )
                                }
                            }
                        }
                    }, onError = Timber::e
                ).addTo(composite)
    }

    /**
     * Control when Stay on call option is chosen
     */
    fun checkStayCall() {
        MegaApplication.getChatManagement().stopCounterToFinishCall()
        MegaApplication.getChatManagement().hasEndCallDialogBeenIgnored = true
        if (_showOnlyMeBanner.value) {
            _showOnlyMeBanner.value = false
            _showWaitingForOthersBanner.value = true
        }

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
        _showOnlyMeBanner.value = false
        _showWaitingForOthersBanner.value = false
        hangCall()

        viewModelScope.launch {
            kotlin.runCatching {
                sendStatisticsMeetingsUseCase(EndCallEmptyCall())
            }
        }
    }

    /**
     * Start the counter to end the call after the previous banner has been hidden
     */
    fun startCounterTimerAfterBanner() {
        MegaApplication.getChatManagement().stopCounterToFinishCall()
        MegaApplication.getChatManagement()
            .startCounterToFinishCall(_state.value.currentChatId)
    }

    /**
     * Method to check if a info banner should be displayed
     */
    fun checkBannerInfo() {
        if (_showPoorConnectionBanner.value) {
            _showPoorConnectionBanner.value = false
            _showPoorConnectionBanner.value = true
        }

        if (_showReconnectingBanner.value) {
            _showReconnectingBanner.value = false
            _showReconnectingBanner.value = true
        }
    }

    /**
     * Method that controls whether to display the bad connection banner
     */
    private fun checkNetworkQualityChanges() {
        networkQualityDisposable?.dispose()
        networkQualityDisposable = getNetworkChangesUseCase.get(_state.value.currentChatId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = {
                    _showPoorConnectionBanner.value =
                        it == GetNetworkChangesUseCase.NetworkQuality.NETWORK_QUALITY_BAD
                },
                onError = Timber::e
            ).addTo(composite)
    }

    /**
     * Method that controls whether to display the reconnecting banner
     */
    private fun checkReconnectingChanges() {
        reconnectingDisposable?.dispose()
        reconnectingDisposable =
            getCallStatusChangesUseCase.getReconnectingStatus(_state.value.currentChatId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onNext = {
                        _showReconnectingBanner.value = it
                    },
                    onError = Timber::e
                ).addTo(composite)
    }

    /**
     * Method to know if this chat is public
     *
     * @return True, if it's public. False, otherwise
     */
    fun isChatRoomPublic(): Boolean =
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let { return it.isPublic }
            ?: false

    /**
     * Method to know if it is the same chat
     *
     * @param chatId chat ID
     * @return True, if it is the same. False, otherwise
     */
    fun isSameChatRoom(chatId: Long): Boolean =
        chatId != MEGACHAT_INVALID_HANDLE && _state.value.currentChatId == chatId

    /**
     * Method to know if it is the same call
     *
     * @param callId call ID
     * @return True, if it is the same. False, otherwise
     */
    fun isSameCall(callId: Long): Boolean =
        _callLiveData.value?.let { it.callId == callId } ?: false

    /**
     * Method to set a call
     *
     * @param chatId chat ID
     */
    fun setCall(chatId: Long, context: Context) {
        if (isSameChatRoom(chatId)) {
            _callLiveData.value = inMeetingRepository.getMeeting(chatId)
            _callLiveData.value?.let {
                if (_updateCallId.value != it.callId) {
                    _updateCallId.value = it.callId
                    checkAnotherCallBanner()
                    checkParticipantsList(context)
                    checkNetworkQualityChanges()
                    checkReconnectingChanges()
                    updateMeetingInfoBottomPanel(context)
                }
            }
        }
    }

    /**
     * Method to get a call
     *
     * @return MegaChatCall
     */
    fun getCall(): MegaChatCall? =
        if (_state.value.currentChatId == MEGACHAT_INVALID_HANDLE) null
        else inMeetingRepository.getChatRoom(_state.value.currentChatId)
            ?.let { inMeetingRepository.getMeeting(it.chatId) }

    /**
     * If it's just me on the call
     *
     * @param chatId chat ID
     * @return True, if it's just me on the call. False, if there are more participants
     */
    fun amIAloneOnTheCall(chatId: Long): Boolean {
        if (isSameChatRoom(chatId)) {
            inMeetingRepository.getMeeting(_state.value.currentChatId)?.let { call ->
                val sessionsInTheCall: MegaHandleList? = call.sessionsClientid
                if (sessionsInTheCall != null && sessionsInTheCall.size() > 0) {
                    Timber.d("I am not the only participant in the call, num of session in the call is ${sessionsInTheCall.size()}")
                    return false
                }

                Timber.d("I am the only participant in the call")
                return true
            }
        }

        Timber.d("I am not the only participant in the call")
        return false
    }

    /**
     * Method to get a chat
     *
     * @return MegaChatRoom
     */
    fun getChat(): MegaChatRoom? = inMeetingRepository.getChatRoom(_state.value.currentChatId)

    /**
     * Method to set a chat
     *
     * @param newChatId chat ID
     */
    fun setChatId(newChatId: Long, context: Context) {
        if (newChatId == MEGACHAT_INVALID_HANDLE || _state.value.currentChatId == newChatId)
            return

        _state.update { state ->
            state.copy(
                currentChatId = newChatId,
            )
        }
        chatManagement.openChatRoom(newChatId)
        getChatRoom()
        getChatCall(context)
        enableAudioLevelMonitor(_state.value.currentChatId)
    }

    /**
     * Enable audio level monitor
     *
     * @param chatId MegaChatHandle of the chat room where enable audio level monitor
     */
    fun enableAudioLevelMonitor(chatId: Long) {
        viewModelScope.launch {
            if (!isAudioLevelMonitorEnabledUseCase(chatId)) {
                enableAudioLevelMonitorUseCase(true, chatId)
            }
        }
    }

    /**
     * Get the chat ID of the current meeting
     *
     * @return chat ID
     */
    fun getChatId(): Long = _state.value.currentChatId

    /**
     *  Method to know if it is a one-to-one chat call
     *
     *  @return True, if it is a one-to-one chat call. False, otherwise
     */
    fun isOneToOneCall(): Boolean = inMeetingRepository.getChatRoom(_state.value.currentChatId)
        ?.let { (!it.isGroup && !it.isMeeting) } ?: false

    /**
     * Set speaker selection automatic or manual
     *
     * @param isAutomatic True, if it's automatic. False, if it's manual
     */
    fun setSpeakerSelection(isAutomatic: Boolean) =
        _state.update {
            it.copy(
                isSpeakerSelectionAutomatic = isAutomatic,
            )
        }

    /**
     * Method to know if it's me
     *
     * @param peerId User handle of a participant
     * @return True, if it's me. False, otherwise
     */
    fun isMe(peerId: Long?): Boolean = inMeetingRepository.isMe(peerId)

    /**
     * Get the session of a participant
     *
     * @param clientId client ID of a participant
     * @return MegaChatSession of a participant
     */
    fun getSession(clientId: Long): MegaChatSession? =
        if (clientId != MEGACHAT_INVALID_HANDLE) _callLiveData.value?.getMegaChatSession(clientId)
        else null

    /**
     * Method to know if a one-to-one call is audio only
     *
     * @return True, if it's audio call. False, otherwise
     */
    fun isAudioCall(): Boolean {
        _callLiveData.value?.let { call ->
            if (call.isOnHold) {
                return true
            }

            val session = getSessionOneToOneCall(call)
            session?.let { sessionParticipant ->
                if (sessionParticipant.isOnHold || (!call.hasLocalVideo() && !MegaApplication.getChatManagement()
                        .getVideoStatus(call.chatid) && !sessionParticipant.hasVideo())
                ) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * Method to know if a call is in progress status
     *
     * @return True, if the chas is in progress. False, otherwise.
     */
    fun isCallEstablished(): Boolean =
        _callLiveData.value?.let { (it.status == CALL_STATUS_IN_PROGRESS) }
            ?: false

    /**
     * Method to know if a call is on hold
     *
     * @return True, if is on hold. False, otherwise
     */
    fun isCallOnHold(): Boolean = _callLiveData.value?.isOnHold ?: false

    /**
     * Method to know if a call or session is on hold in meeting
     *
     * @return True, if is on hold. False, otherwise
     */
    fun isCallOrSessionOnHold(clientId: Long): Boolean =
        if (isCallOnHold()) true
        else getSession(clientId)?.isOnHold ?: false

    /**
     * Method to know if a call or session is on hold in one to one call
     *
     * @return True, if is on hold. False, otherwise
     */
    fun isCallOrSessionOnHoldOfOneToOneCall(): Boolean =
        if (isCallOnHold()) true else isSessionOnHoldOfOneToOneCall()

    /**
     * Method to know if a session is on hold in one to one call
     *
     * @return True, if is on hold. False, otherwise
     */
    private fun isSessionOnHoldOfOneToOneCall(): Boolean {
        _callLiveData.value?.let { call ->
            if (isOneToOneCall()) {
                val session = inMeetingRepository.getSessionOneToOneCall(call)
                session?.let {
                    return it.isOnHold
                }
            }
        }

        return false
    }

    /**
     * Method to obtain a specific call
     *
     * @param chatId Chat ID
     * @return MegaChatCall the another call
     */
    private fun getAnotherCall(chatId: Long): MegaChatCall? =
        if (chatId == MEGACHAT_INVALID_HANDLE) null else inMeetingRepository.getMeeting(chatId)

    /**
     * Method to know if exists another call in progress or on hold.
     *
     * @return MegaChatCall the another call
     */
    fun getAnotherCall(): MegaChatCall? {
        val anotherCallChatId = CallUtil.getAnotherCallParticipating(_state.value.currentChatId)
        if (anotherCallChatId != MEGACHAT_INVALID_HANDLE) {
            val anotherCall = inMeetingRepository.getMeeting(anotherCallChatId)
            anotherCall?.let {
                if (isCallOnHold() && !it.isOnHold) {
                    Timber.d("This call in on hold, another call in progress")
                    return anotherCall
                }

                if (!isCallOnHold() && it.isOnHold) {
                    Timber.d("This call in progress, another call on hold")
                    return anotherCall
                }
            }

        }

        Timber.d("No other calls in progress or on hold")
        return null
    }

    /**
     * Get session of a contact in a one-to-one call
     *
     * @param callChat MegaChatCall
     */
    fun getSessionOneToOneCall(callChat: MegaChatCall?): MegaChatSession? =
        callChat?.getMegaChatSession(callChat.sessionsClientid[0])

    /**
     * Method to obtain the full name of a participant
     *
     * @param peerId User handle of a participant
     * @return The name of a participant
     */
    fun getParticipantFullName(peerId: Long): String =
        CallUtil.getUserNameCall(MegaApplication.getInstance().applicationContext, peerId)

    /**
     * Method to find out if there is a participant in the call
     *
     * @param peerId Use handle of a participant
     * @param typeChange the type of change, name or avatar
     * @return list of participants with changes
     */
    fun updateParticipantsNameOrAvatar(
        peerId: Long,
        typeChange: Int,
        context: Context,
    ): MutableSet<Participant> {
        val listWithChanges = mutableSetOf<Participant>()
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            participants.value = participants.value?.map { participant ->
                return@map when {
                    participant.peerId == peerId && typeChange == NAME_CHANGE -> {
                        listWithChanges.add(participant)
                        participant.copy(
                            name = getParticipantFullName(peerId),
                            avatar = getAvatarBitmap(peerId)
                        )
                    }

                    participant.peerId == peerId && typeChange == AVATAR_CHANGE -> {
                        listWithChanges.add(participant)
                        participant.copy(avatar = getAvatarBitmap(peerId))
                    }

                    else -> participant
                }
            }?.toMutableList()
            updateMeetingInfoBottomPanel(context)
        }
        return listWithChanges
    }

    /**
     * Method that makes the necessary changes to the participant list when my own privileges have changed.
     */
    fun updateOwnPrivileges(context: Context) {
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            participants.value = participants.value?.map { participant ->
                return@map participant.copy(
                    hasOptionsAllowed = shouldParticipantsOptionBeVisible(
                        participant.isMe,
                        participant.isGuest
                    )
                )
            }?.toMutableList()
            updateMeetingInfoBottomPanel(context)
        }
    }

    /**
     * Method for updating participant privileges
     *
     * @return list of participants with changes
     */
    fun updateParticipantsPrivileges(context: Context): MutableSet<Participant> {
        val listWithChanges = mutableSetOf<Participant>()
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            participants.value = participants.value?.map { participant ->
                return@map when {
                    participant.isModerator != isParticipantModerator(participant.peerId) -> {
                        listWithChanges.add(participant)
                        participant.copy(isModerator = isParticipantModerator(participant.peerId))
                    }

                    else -> participant
                }
            }?.toMutableList()
            updateMeetingInfoBottomPanel(context)
        }

        return listWithChanges
    }

    /**
     * Method to switch a call on hold
     *
     * @param isCallOnHold True, if I am going to put it on hold. False, otherwise
     */
    fun setCallOnHold(isCallOnHold: Boolean) {
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            inMeetingRepository.setCallOnHold(it.chatId, isCallOnHold)
        }
    }

    /**
     * Method to switch another call on hold
     *
     * @param chatId chat ID
     * @param isCallOnHold True, if I am going to put it on hold. False, otherwise
     */
    fun setAnotherCallOnHold(chatId: Long, isCallOnHold: Boolean) {
        inMeetingRepository.getChatRoom(chatId)?.let {
            inMeetingRepository.setCallOnHold(it.chatId, isCallOnHold)
        }
    }

    /**
     * Method to know if the session of a participants is null
     *
     * @param clientId The client ID of a participant
     */
    fun isSessionOnHold(clientId: Long): Boolean = getSession(clientId)?.isOnHold ?: false

    /**
     * Method for displaying the correct banner: If the call is muted or on hold
     *
     * @param bannerIcon The icon of the banner
     * @param bannerText The textView of the banner
     * @return The text of the banner
     */
    fun showAppropriateBanner(bannerIcon: ImageView?, bannerText: EmojiTextView?): Boolean {
        //Check call or session on hold
        if (isCallOnHold() || isSessionOnHoldOfOneToOneCall()) {
            bannerIcon?.let {
                it.isVisible = false
            }
            bannerText?.let {
                it.text = it.context.getString(R.string.call_on_hold)
            }
            return true
        }

        //Check mute call or session
        _callLiveData.value?.let { call ->
            if (isOneToOneCall()) {
                inMeetingRepository.getSessionOneToOneCall(call)?.let { session ->
                    if (!session.hasAudio() && session.peerid != MEGACHAT_INVALID_HANDLE) {
                        bannerIcon?.let {
                            it.isVisible = true
                        }
                        bannerText?.let {
                            it.text = it.context.getString(
                                R.string.muted_contact_micro,
                                inMeetingRepository.getContactOneToOneCallName(
                                    session.peerid
                                )
                            )
                        }
                        return true
                    }
                }
            }

            if (!call.hasLocalAudio()) {
                bannerIcon?.let {
                    it.isVisible = false
                }
                bannerText?.let {
                    it.text =
                        it.context.getString(R.string.muted_own_micro)
                }
                return true
            }
        }

        return false
    }

    /**
     *  Method to know if it is a outgoing call
     *
     *  @return True, if it is a outgoing call. False, otherwise
     */
    fun isRequestSent(): Boolean {
        val callId = _callLiveData.value?.callId ?: return false

        return callId != MEGACHAT_INVALID_HANDLE && MegaApplication.getChatManagement()
            .isRequestSent(callId)
    }

    /**
     * Method for determining whether to display the camera switching icon.
     *
     * @return True, if it is. False, if not.
     */
    fun isNecessaryToShowSwapCameraOption(): Boolean =
        _callLiveData.value?.let { it.status != CALL_STATUS_CONNECTING && it.hasLocalVideo() && !it.isOnHold }
            ?: false


    /**
     * Start chat call
     *
     * @param enableVideo The video should be enabled
     * @param enableAudio The audio should be enabled
     * @return Chat id
     */
    fun startMeeting(
        enableVideo: Boolean,
        enableAudio: Boolean,
    ): LiveData<Long> {
        val chatIdResult = MutableLiveData<Long>()
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            Timber.d("The chat exists")
            if (CallUtil.isStatusConnected(
                    MegaApplication.getInstance().applicationContext,
                    it.chatId
                )
            ) {
                megaChatApiGateway.getChatCall(_state.value.currentChatId)?.let {
                    Timber.d("There is a call, open it")
                    chatIdResult.value = it.chatid
                    CallUtil.openMeetingInProgress(
                        MegaApplication.getInstance().applicationContext,
                        _state.value.currentChatId,
                        true,
                        passcodeManagement
                    )

                    return chatIdResult
                }

                Timber.d("Chat status is connected")
                MegaApplication.isWaitingForCall = false

                viewModelScope.launch {
                    runCatching {
                        setChatVideoInDeviceUseCase()
                        startChatCall(_state.value.currentChatId, enableVideo, enableAudio)
                    }.onFailure { exception ->
                        Timber.e(exception)
                    }.onSuccess { resultStartCall ->
                        val chatId = resultStartCall.chatHandle
                        if (chatId != MEGACHAT_INVALID_HANDLE) {
                            chatManagement.setSpeakerStatus(chatId, resultStartCall.flag)
                            megaChatApiGateway.getChatCall(chatId)?.let { call ->
                                if (call.isOutgoing) {
                                    chatManagement.setRequestSentCall(call.callId, true)
                                }
                            }

                            chatIdResult.value = chatId
                        }
                    }
                }
            }
            return chatIdResult
        }

        Timber.d("The chat doesn't exists")
        inMeetingRepository.createMeeting(
            _state.value.chatTitle,
            CreateGroupChatWithPublicLink()
        )

        return chatIdResult
    }


    /**
     * Get my own privileges in the chat
     *
     * @return the privileges
     */
    fun getOwnPrivileges(): Int = inMeetingRepository.getOwnPrivileges(_state.value.currentChatId)

    /**
     * Method to know if the participant is a moderator.
     *
     * @param peerId User handle of a participant
     */
    private fun isParticipantModerator(peerId: Long): Boolean =
        if (isMe(peerId))
            getOwnPrivileges() == PRIV_MODERATOR
        else
            inMeetingRepository.getChatRoom(_state.value.currentChatId)
                ?.let { it.getPeerPrivilegeByHandle(peerId) == PRIV_MODERATOR }
                ?: false

    /**
     * Method to know if the participant is my contact
     *
     * @param peerId User handle of a participant
     */
    private fun isMyContact(peerId: Long): Boolean =
        if (isMe(peerId))
            true
        else
            inMeetingRepository.isMyContact(peerId)

    /**
     * Method to update whether a user is my contact or not
     *
     * @param peerId User handle
     */
    fun updateParticipantsVisibility(peerId: Long) {
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            participants.value?.let { listParticipants ->
                val iterator = listParticipants.iterator()
                iterator.forEach {
                    if (it.peerId == peerId) {
                        it.isContact = isMyContact(peerId)
                    }
                }
            }
        }
    }

    /**
     * Method for updating the speaking participant
     *
     * @param newSpeakerPeerId User handle of a participant
     * @param newSpeakerClientId Client ID of a participant
     * @return list of participants with changes
     */
    fun updatePeerSelected(
        newSpeakerPeerId: Long,
        newSpeakerClientId: Long,
    ): MutableSet<Participant> {
        val listWithChanges = mutableSetOf<Participant>()
        participants.value?.forEach {
            when {
                it.isSpeaker && (it.peerId != newSpeakerPeerId || it.clientId != newSpeakerClientId) -> {
                    Timber.d("The previous speaker ${it.clientId}, now has isSpeaker false")
                    it.isSpeaker = false
                    listWithChanges.add(it)
                }

                !it.isSpeaker && it.peerId == newSpeakerPeerId && it.clientId == newSpeakerClientId -> {
                    Timber.d("New speaker selected found ${it.clientId}")
                    it.isSpeaker = true
                    addSpeaker(it)
                    listWithChanges.add(it)
                }
            }
        }

        return listWithChanges
    }

    /**
     * Check screens shared
     */
    fun checkScreensShared() {
        if (_state.value.callUIStatus == CallUIStatusType.SpeakerView) {
            val addScreensSharedParticipantsList = mutableSetOf<Participant>()
            participants.value?.filter { !it.isSpeaker }?.forEach {
                getSession(it.clientId)?.apply {
                    if (hasScreenShare() && !participantHasScreenSharedParticipant(it)) {
                        addScreensSharedParticipantsList.add(it)
                    }
                }
            }

            _state.update { state -> state.copy(addScreensSharedParticipantsList = addScreensSharedParticipantsList.toMutableList()) }
        }
        val removeScreensSharedParticipantsList = mutableSetOf<Participant>()
        participants.value?.filter { it.isSpeaker }?.forEach {
            getSession(it.clientId)?.apply {
                if (hasScreenShare() && participantHasScreenSharedParticipant(it)) {
                    removeScreensSharedParticipantsList.add(it)
                }
            }
        }

        _state.update { state -> state.copy(removeScreensSharedParticipantsList = removeScreensSharedParticipantsList.toMutableList()) }
    }

    /**
     * Remove screen shared participant
     *
     * @param list  List of [Participant]
     * @param context   Context
     */
    fun removeScreenShareParticipant(list: List<Participant>?, context: Context): Int? {
        _state.update { state -> state.copy(removeScreensSharedParticipantsList = null) }
        list?.forEach { user ->
            getSession(user.clientId)?.let { session ->
                participants.value?.indexOf(user)?.let { position ->
                    if (position != INVALID_POSITION) {
                        participants.value?.get(position)?.let { participant ->
                            if (participant.isVideoOn) {
                                participant.videoListener?.let { listener ->
                                    removeResolutionAndListener(participant, listener)
                                }
                                participant.videoListener = null
                            }
                        }
                        participants.value?.removeAt(position)
                        Timber.d("Removing participant")
                        updateParticipantsList(context)
                        return position
                    }
                }
            }
        }
        return INVALID_POSITION
    }

    /**
     * Add screen shared participant
     *
     * @param list  List of [Participant]
     * @param context   Context
     * @return  Position of the screen shared
     */
    fun addScreenShareParticipant(list: List<Participant>?, context: Context): Int? {
        _state.update { state -> state.copy(addScreensSharedParticipantsList = null) }
        list?.forEach { participant ->
            createParticipant(
                isScreenShared = true,
                participant.clientId
            )?.let { screenSharedParticipant ->
                participants.value?.indexOf(participant)?.let { index ->
                    participants.value?.add(index, screenSharedParticipant)
                    updateParticipantsList(context)
                    return participants.value?.indexOf(screenSharedParticipant)
                }
            }
        }
        return INVALID_POSITION
    }

    /**
     * Method for create a participant
     *
     * @param isScreenShared True if it's the screen shared. False if not.
     * @param clientId  Client Id.
     * @return [Participant]
     */
    private fun createParticipant(isScreenShared: Boolean, clientId: Long): Participant? {
        _state.value.call?.apply {
            sessionByClientId[clientId]?.let { session ->
                when {
                    isScreenShared ->
                        participants.value?.filter { it.peerId == session.peerId && it.clientId == session.clientId && it.isScreenShared }
                            ?.let {
                                if (it.isNotEmpty()) {
                                    Timber.d("Screen shared already shown")
                                    return null
                                }
                            }

                    else ->
                        participants.value?.filter { it.peerId == session.peerId && it.clientId == session.clientId }
                            ?.let {
                                if (it.isNotEmpty()) {
                                    Timber.d("Participants already shown")
                                    return null
                                }
                            }
                }

                val isModerator = isParticipantModerator(session.peerId)
                val name = getParticipantName(session.peerId)
                val isContact = isMyContact(session.peerId)
                val hasHiRes = needHiRes()

                val avatar = inMeetingRepository.getAvatarBitmap(session.peerId)
                val email = inMeetingRepository.getEmailParticipant(
                    session.peerId,
                    GetUserEmailListener(
                        MegaApplication.getInstance().applicationContext,
                        this@InMeetingViewModel
                    )
                )
                val isGuest = email == null

                val isSpeaker = getCurrentSpeakerParticipant()?.let { participant ->
                    participant.clientId == session.clientId && participant.peerId == session.peerId && participant.isSpeaker
                } ?: false


                return Participant(
                    peerId = session.peerId,
                    clientId = session.clientId,
                    name = name,
                    avatar = avatar,
                    isMe = false,
                    isModerator = isModerator,
                    isAudioOn = session.hasAudio,
                    isVideoOn = session.hasVideo,
                    isAudioDetected = session.isAudioDetected,
                    isContact = isContact,
                    isSpeaker = isSpeaker,
                    hasHiRes = if (isScreenShared) true else hasHiRes,
                    videoListener = null,
                    isChosenForAssign = false,
                    isGuest = isGuest,
                    hasOptionsAllowed = shouldParticipantsOptionBeVisible(false, isGuest),
                    isPresenting = session.hasScreenShare,
                    isScreenShared = isScreenShared
                )
            }
        }
        return null
    }

    /**
     * Method that creates the participant speaker
     *
     * @param participant The participant who is to be a speaker
     * @return speaker participant
     */
    private fun createSpeakerParticipant(participant: Participant): Participant = Participant(
        participant.peerId,
        participant.clientId,
        participant.name,
        participant.avatar,
        isMe = participant.isMe,
        isModerator = participant.isModerator,
        isAudioOn = participant.isAudioOn,
        isVideoOn = participant.isVideoOn,
        isAudioDetected = participant.isAudioDetected,
        isContact = participant.isContact,
        isSpeaker = true,
        hasHiRes = true,
        videoListener = null,
        participant.isChosenForAssign,
        participant.isGuest,
        isPresenting = participant.isPresenting,
        isScreenShared = false
    )

    /**
     * Method to update the current participants list
     */
    fun checkParticipantsList(context: Context) {
        callLiveData.value?.let {
            val participants: Int = participants.value?.size ?: 0
            if (it.sessionsClientid.size() > 0 && (participants.toLong() != it.sessionsClientid.size())) {
                createCurrentParticipants(it.sessionsClientid, context)
            }
        }
    }

    /**
     * Method for creating participants already on the call
     *
     * @param list list of participants
     */
    private fun createCurrentParticipants(list: MegaHandleList?, context: Context) {
        list?.let { listParticipants ->
            participants.value?.clear()
            if (listParticipants.size() > 0) {
                for (i in 0 until list.size()) {
                    createParticipant(isScreenShared = false, list[i])?.let { participantCreated ->
                        participants.value?.add(participantCreated)
                    }
                }

                updateParticipantsList(context)
            }
        }
    }

    /**
     * Method to control when the number of participants changes
     */
    private fun updateParticipantsList(context: Context) {
        participants.value = participants.value
        updateMeetingInfoBottomPanel(context)
    }

    /**
     * Method for adding a participant to the list
     *
     * @param clientId  Client Id
     * @return the position of the participant
     */
    fun addParticipant(clientId: Long, context: Context): Int? {
        createParticipant(isScreenShared = false, clientId)?.let { participantCreated ->
            participants.value?.add(participantCreated)
            updateParticipantsList(context)

            val currentSpeaker = getCurrentSpeakerParticipant()
            if (currentSpeaker == null) {
                getFirstParticipant(
                    MEGACHAT_INVALID_HANDLE,
                    MEGACHAT_INVALID_HANDLE
                )?.apply {
                    updatePeerSelected(peerId, clientId)
                }
            }

            return participants.value?.indexOf(participantCreated)
        }

        return INVALID_POSITION
    }

    /**
     * Method for removing the listener from participants who still have
     */
    fun removeListeners() {
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            val iterator = participants.value?.iterator()
            iterator?.let { list ->
                list.forEach { participant ->
                    participant.videoListener?.let { listener ->
                        removeResolutionAndListener(participant, listener)
                        participant.videoListener = null
                    }
                }
            }
        }
    }

    /**
     * Method for removing a participant
     *
     * @param session MegaChatSession of a participant
     * @return the position of the participant
     */
    fun removeParticipant(session: MegaChatSession, context: Context): Int {
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            val iterator = participants.value?.iterator()
            iterator?.let { list ->
                list.forEach { participant ->
                    if (participant.peerId == session.peerid && participant.clientId == session.clientid) {
                        val position = participants.value?.indexOf(participant)
                        val clientId = participant.clientId
                        val isSpeaker = participant.isSpeaker
                        participant.isSpeaker = false

                        if (position != null && position != INVALID_POSITION) {
                            if (participant.isVideoOn) {
                                participant.videoListener?.let { listener ->
                                    removeResolutionAndListener(participant, listener)
                                }
                                participant.videoListener = null
                            }

                            participants.value?.removeAt(position)
                            Timber.d("Removing participant... $clientId")
                            updateParticipantsList(context)

                            if (isSpeaker) {
                                Timber.d("The removed participant was speaker, clientID ${participant.clientId}")
                                removePreviousSpeakers()
                                removeCurrentSpeaker()
                            }
                            return position
                        }
                    }
                }
            }
        }

        return INVALID_POSITION
    }

    /**
     * Stop remote video resolution of participant in a meeting.
     *
     * @param participant The participant from whom the video is to be closed
     */
    fun removeRemoteVideoResolution(participant: Participant) {
        if (participant.videoListener == null) return

        getSession(participant.clientId)?.let {
            if (participant.hasHiRes && it.canRecvVideoHiRes()) {
                Timber.d("Stop HiResolution and remove listener, clientId = ${participant.clientId}")
                stopHiResVideo(it, _state.value.currentChatId)

            } else if (!participant.hasHiRes && it.canRecvVideoLowRes()) {
                Timber.d("Stop LowResolution and remove listener, clientId = ${participant.clientId}")
                stopLowResVideo(it, _state.value.currentChatId)
            }
        }
    }

    /**
     * Remove remote video listener of participant in a meeting.
     *
     * @param participant The participant from whom the video is to be closed
     */
    fun removeRemoteVideoListener(
        participant: Participant,
        listener: MegaChatVideoListenerInterface,
    ) {
        Timber.d("Remove the remote video listener of clientID ${participant.clientId}")
        removeChatRemoteVideoListener(
            listener,
            participant.clientId,
            _state.value.currentChatId,
            participant.hasHiRes
        )
    }

    /**
     * Close Video of participant in a meeting. Removing resolution and listener.
     *
     * @param participant The participant from whom the video is to be closed
     */
    fun removeResolutionAndListener(
        participant: Participant,
        listener: MegaChatVideoListenerInterface,
    ) {
        if (participant.videoListener == null) return

        removeRemoteVideoResolution(participant)
        removeRemoteVideoListener(participant, listener)
    }

    /**
     * Method to create the GroupVideoListener
     *
     * @param participant The participant whose listener is to be created
     * @param alpha Alpha for TextureView
     * @param rotation Rotation for TextureView
     */
    fun createVideoListener(
        participant: Participant,
        alpha: Float,
        rotation: Float,
    ): GroupVideoListener {
        val myTexture = TextureView(MegaApplication.getInstance().applicationContext)
        myTexture.layoutParams = RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        myTexture.alpha = alpha
        myTexture.rotation = rotation

        return GroupVideoListener(
            myTexture,
            participant.peerId,
            participant.clientId,
            participant.isMe
        )
    }

    /**
     * Method for know if the resolution of a participant's video should be high
     *
     * @return True, if should be high. False, otherwise
     */
    private fun needHiRes(): Boolean =
        participants.value?.let { state.value.callUIStatus != CallUIStatusType.SpeakerView }
            ?: false

    /**
     * Method to know if the session has video on and is not on hold
     *
     * @param clientId Client ID of participant
     * @return True, it does. False, if not.
     */
    fun sessionHasVideo(clientId: Long): Boolean =
        getSession(clientId)?.let { it.hasVideo() && !isCallOrSessionOnHold(it.clientid) && it.status == MegaChatSession.SESSION_STATUS_IN_PROGRESS }
            ?: false

    /**
     * Method for get the participant name
     *
     * @param peerId user handle
     * @return the name of a participant
     */
    private fun getParticipantName(peerId: Long): String =
        if (isMe(peerId))
            inMeetingRepository.getMyFullName()
        else
            inMeetingRepository.participantName(peerId) ?: " "

    /**
     * Method that marks a participant as a non-speaker
     *
     * @param peerId User handle of a participant
     * @param clientId Client ID of a participant
     */
    fun removeSelected(peerId: Long, clientId: Long) {
        val iterator = participants.value?.iterator()
        iterator?.let { participant ->
            participant.forEach {
                if (it.peerId == peerId && it.clientId == clientId && it.isSpeaker) {
                    it.isSpeaker = false
                }
            }
        }
    }

    /**
     * Get the avatar
     *
     * @param peerId User handle of a participant
     * @return the avatar of a participant
     */
    fun getAvatarBitmap(peerId: Long): Bitmap? =
        inMeetingRepository.getChatRoom(_state.value.currentChatId)
            ?.let { inMeetingRepository.getAvatarBitmap(peerId) }

    /**
     * Method to get the first participant in the list, who will be the new speaker
     */
    fun getFirstParticipant(peerId: Long, clientId: Long): Participant? {
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            if (participants.value != null && (participants.value ?: return@let).size > 0) {
                if (peerId == MEGACHAT_INVALID_HANDLE && clientId == MEGACHAT_INVALID_HANDLE) {
                    return (participants.value ?: return@let)[0]
                }

                val iterator = participants.value?.iterator()
                iterator?.let { participant ->
                    participant.forEach {
                        if (it.peerId != peerId || it.clientId != clientId) {
                            return it
                        }
                    }
                }
            }
        }

        return null
    }

    /**
     * Get participant from peerId and clientId
     *
     * @param peerId peer ID of a participant
     * @param clientId client ID of a participant
     */
    fun getParticipant(peerId: Long, clientId: Long): Participant? {
        participants.value?.let { list ->
            val participants = list.filter {
                it.peerId == peerId && it.clientId == clientId && !it.isScreenShared
            }

            if (participants.isNotEmpty()) {
                return participants[0]
            }
        }

        return null
    }

    /**
     * Method for updating participant video
     *
     * @param session of a participant
     * @return True, if there have been changes. False, otherwise
     */
    fun changesInRemoteVideoFlag(session: MegaChatSession): Boolean {
        var hasChanged = false
        participants.value = participants.value?.map { participant ->
            return@map when {
                participant.peerId == session.peerid && participant.clientId == session.clientid && participant.isVideoOn != session.hasVideo() -> {
                    hasChanged = true
                    participant.copy(isVideoOn = session.hasVideo())
                }

                else -> participant
            }
        }?.toMutableList()

        speakerParticipants.value = speakerParticipants.value?.map { participant ->
            return@map when {
                participant.peerId == session.peerid && participant.clientId == session.clientid && participant.isVideoOn != session.hasVideo() -> {
                    hasChanged = true
                    participant.copy(isVideoOn = session.hasVideo())
                }

                else -> participant
            }
        }?.toMutableList()

        return hasChanged
    }

    /**
     * Method for updating participant screen sharing
     *
     * @param session of a participant
     * @return True, if there have been changes. False, otherwise
     */
    fun changesInScreenSharing(session: MegaChatSession): Boolean {
        var hasChanged = false
        participants.value = participants.value?.map { participant ->
            return@map when {
                participant.peerId == session.peerid && participant.clientId == session.clientid && participant.isPresenting != session.hasScreenShare() -> {
                    hasChanged = true
                    participant.copy(isPresenting = session.hasScreenShare())
                }

                else -> participant
            }
        }?.toMutableList()

        speakerParticipants.value = speakerParticipants.value?.map { participant ->
            return@map when {
                participant.peerId == session.peerid && participant.clientId == session.clientid && participant.isPresenting != session.hasScreenShare() -> {
                    hasChanged = true
                    participant.copy(isPresenting = session.hasScreenShare())
                }

                else -> participant
            }
        }?.toMutableList()
        return hasChanged
    }

    /**
     * Method for updating participant audio
     *
     * @param session of a participant
     * @return True, if there have been changes. False, otherwise
     */
    fun changesInRemoteAudioFlag(session: MegaChatSession): Boolean {
        var hasChanged = false
        participants.value = participants.value?.map { participant ->
            return@map when {
                participant.peerId == session.peerid && participant.clientId == session.clientid &&
                        (participant.isAudioOn != session.hasAudio() || participant.isAudioDetected != session.isAudioDetected) -> {
                    hasChanged = true
                    participant.copy(isAudioOn = session.hasAudio())
                }

                else -> participant
            }
        }?.toMutableList()

        return hasChanged
    }

    /**
     * Method that makes the necessary checks before joining a meeting.
     * If there is another call, it must be put on hold.
     * If there are two other calls, the one in progress is hung up.
     *
     * @param chatIdOfCurrentCall chat id of current call
     */
    fun checkAnotherCallsInProgress(chatIdOfCurrentCall: Long) {
        val numCallsParticipating = CallUtil.getCallsParticipating()
        numCallsParticipating?.let {
            if (numCallsParticipating.isEmpty()) {
                return
            }

            if (numCallsParticipating.size == 1) {
                getAnotherCall(numCallsParticipating[0])?.let { anotherCall ->
                    if (chatIdOfCurrentCall != anotherCall.chatid && !anotherCall.isOnHold) {
                        Timber.d("Another call on hold before join the meeting")
                        setAnotherCallOnHold(anotherCall.chatid, true)
                    }
                }
            } else {
                for (i in 0 until numCallsParticipating.size) {
                    getAnotherCall(numCallsParticipating[i])?.let { anotherCall ->
                        if (chatIdOfCurrentCall != anotherCall.chatid && !anotherCall.isOnHold) {
                            Timber.d("Hang up one of the current calls in order to join the meeting")
                            hangUpSpecificCall(anotherCall.callId)
                        }
                    }
                }
            }
        }
    }

    /**
     * Method for ignore a call
     */
    fun ignoreCall() {
        _callLiveData.value?.let {
            inMeetingRepository.ignoreCall(it.chatid)
        }
    }

    /**
     * Method for remove incoming call notification
     */
    fun removeIncomingCallNotification(chatId: Long) {
        inMeetingRepository.getMeeting(chatId)?.let { call ->
            rtcAudioManagerGateway.stopSounds()
            CallUtil.clearIncomingCallNotification(call.callId)
        }
    }

    /**
     * Method to hang up a specific call
     *
     * @param callId Call ID
     */
    private fun hangUpSpecificCall(callId: Long) {
        hangCall(callId)
    }

    /**
     * Set a title for the chat
     *
     * @param newTitle the chat title
     */
    fun setTitleChat(newTitle: String) {
        if (_state.value.currentChatId == MEGACHAT_INVALID_HANDLE) {
            _state.update { it.copy(chatTitle = newTitle) }
        } else {
            inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
                inMeetingRepository.setTitleChatRoom(
                    it.chatId,
                    newTitle,
                    EditChatRoomNameListener(MegaApplication.getInstance(), this)
                )
            }
        }
    }

    /**
     * Method of obtaining the remote video
     *
     * @param listener MegaChatVideoListenerInterface
     * @param clientId Client ID of participant
     * @param chatId Chat ID
     * @param isHiRes True, if it has HiRes. False, if it has LowRes
     */
    fun addChatRemoteVideoListener(
        listener: MegaChatVideoListenerInterface,
        clientId: Long,
        chatId: Long,
        isHiRes: Boolean,
    ) {
        Timber.d("Adding remote video listener, clientId $clientId, isHiRes $isHiRes")
        inMeetingRepository.addChatRemoteVideoListener(
            chatId,
            clientId,
            isHiRes,
            listener
        )
    }

    /**
     * Method of remove the remote video
     *
     * @param listener MegaChatVideoListenerInterface
     * @param clientId Client ID of participant
     * @param chatId Chat ID
     * @param isHiRes True, if it has HiRes. False, if it has LowRes
     */
    fun removeChatRemoteVideoListener(
        listener: MegaChatVideoListenerInterface,
        clientId: Long,
        chatId: Long,
        isHiRes: Boolean,
    ) {
        Timber.d("Removing remote video listener, clientId $clientId, isHiRes $isHiRes")
        inMeetingRepository.removeChatRemoteVideoListener(
            chatId,
            clientId,
            isHiRes,
            listener
        )
    }

    /**
     * Add High Resolution for remote video
     *
     * @param session MegaChatSession of a participant
     * @param chatId Chat ID
     */
    fun requestHiResVideo(
        session: MegaChatSession?,
        chatId: Long,
    ) {
        session?.apply {
            if (!canRecvVideoHiRes() && isHiResVideo) {
                Timber.d("Adding HiRes for remote video, clientId ${clientid}")
                viewModelScope.launch {
                    runCatching {
                        requestHighResolutionVideoUseCase(chatId, clientid)
                    }.onFailure { exception ->
                        Timber.e(exception)
                    }.onSuccess { request ->
                        Timber.d("Request high res video: chatId = ${request.chatHandle}, hires? ${request.flag}, clientId = ${request.userHandle}")
                    }
                }
            }
        }
    }

    /**
     * Remove High Resolution for remote video
     *
     * @param session MegaChatSession of a participant
     * @param chatId Chat ID
     */
    fun stopHiResVideo(
        session: MegaChatSession?,
        chatId: Long,
    ) {
        session?.let { sessionParticipant ->
            if (sessionParticipant.canRecvVideoHiRes()) {
                Timber.d("Removing HiRes for remote video, clientId ${sessionParticipant.clientid}")
                viewModelScope.launch {
                    runCatching {
                        stopHighResolutionVideoUseCase(chatId, sessionParticipant.clientid)
                    }.onFailure { exception ->
                        Timber.e(exception)
                    }.onSuccess { request ->
                        Timber.d("Stop high res video: chatId = ${request.chatHandle}, hires? ${request.flag}, clientId = ${request.userHandle}")
                    }
                }
            }
        }
    }

    /**
     * Add Low Resolution for remote video
     *
     * @param session MegaChatSession of a participant
     * @param chatId Chat ID
     */
    fun requestLowResVideo(
        session: MegaChatSession?,
        chatId: Long,
    ) {
        session?.let { sessionParticipant ->
            if (!sessionParticipant.canRecvVideoLowRes() && sessionParticipant.isLowResVideo) {
                Timber.d("Adding LowRes for remote video, clientId ${sessionParticipant.clientid}")
                viewModelScope.launch {
                    runCatching {
                        requestLowResolutionVideoUseCase(chatId, sessionParticipant.clientid)
                    }.onFailure { exception ->
                        Timber.e(exception)
                    }.onSuccess { request ->
                        Timber.d("Request low res video: chatId = ${request.chatHandle}, lowRes? ${request.flag}, clientId = ${request.userHandle}")
                    }
                }
            }
        }
    }

    /**
     * Remove Low Resolution for remote video
     *
     * @param session MegaChatSession of a participant
     * @param chatId Chat ID
     */
    private fun stopLowResVideo(
        session: MegaChatSession?,
        chatId: Long,
    ) {
        session?.let { sessionParticipant ->
            if (sessionParticipant.canRecvVideoLowRes()) {
                Timber.d("Removing LowRes for remote video, clientId ${sessionParticipant.clientid}")
                viewModelScope.launch {
                    runCatching {
                        stopLowResolutionVideoUseCase(chatId, sessionParticipant.clientid)
                    }.onFailure { exception ->
                        Timber.e(exception)
                    }.onSuccess { request ->
                        Timber.d("Stop low res video: chatId = ${request.chatHandle}, lowRes? ${request.flag}, clientId = ${request.userHandle}")
                    }
                }
            }
        }
    }

    /**
     * Method for checking which participants need to change their resolution when the UI is changed
     *
     * In Speaker view, the list of participants should have low res
     * In Grid view, if there is more than 4, low res. Hi res in the opposite case
     */
    fun updateParticipantResolution() {
        Timber.d("Changing the resolution of participants when the UI changes")
        participants.value?.let { listParticipants ->
            val iterator = listParticipants.iterator()
            iterator.forEach { participant ->
                getSession(participant.clientId)?.let {
                    if (state.value.callUIStatus == CallUIStatusType.SpeakerView && participant.hasHiRes && !participant.isScreenShared) {
                        Timber.d("Change to low resolution, clientID ${participant.clientId}")
                        participant.videoListener?.let {
                            removeResolutionAndListener(participant, it)
                        }

                        participant.videoListener = null
                        participant.hasHiRes = false
                    } else if (state.value.callUIStatus == CallUIStatusType.GridView && !participant.hasHiRes) {
                        Timber.d("Change to high resolution, clientID ${participant.clientId}")
                        participant.videoListener?.let {
                            removeResolutionAndListener(participant, it)
                        }
                        participant.videoListener = null
                        participant.hasHiRes = true
                    }
                }
            }
        }
    }

    /**
     * Adding visible participant
     *
     * @param participant The participant that is now visible
     */
    fun addParticipantVisible(participant: Participant) {
        if (visibleParticipants.size == 0) {
            visibleParticipants.add(participant)
            return
        }

        val checkParticipant = visibleParticipants.filter {
            it.peerId == participant.peerId && it.clientId == participant.clientId
        }

        if (checkParticipant.isEmpty()) {
            visibleParticipants.add(participant)
        }
    }

    /**
     * Removing all visible participants
     */
    fun removeAllParticipantVisible() {
        if (visibleParticipants.isEmpty()) {
            return
        }

        visibleParticipants.clear()
    }

    /**
     * Removing visible participant
     *
     * @param participant The participant that is not now visible
     */
    fun removeParticipantVisible(participant: Participant) {
        if (visibleParticipants.size == 0) {
            return
        }
        val checkParticipant = visibleParticipants.filter {
            it.peerId == participant.peerId && it.clientId == participant.clientId
        }
        if (checkParticipant.isNotEmpty()) {
            visibleParticipants.remove(participant)
        }
    }

    /**
     * Check if a participant is visible
     *
     * @param participant The participant to be checked whether or not he/she is visible
     * @return True, if it's visible. False, otherwise
     */
    fun isParticipantVisible(participant: Participant): Boolean {
        if (visibleParticipants.isNotEmpty()) {
            val participantVisible = visibleParticipants.filter {
                it.peerId == participant.peerId && it.clientId == participant.clientId
            }

            if (participantVisible.isNotEmpty()) {
                return true
            }
        }

        return false
    }

    /**
     * Updating visible participants list
     *
     * @param list new list of visible participants
     */
    fun updateVisibleParticipants(list: List<Participant>?) {
        if (!list.isNullOrEmpty()) {
            val iteratorParticipants = list.iterator()
            iteratorParticipants.forEach { participant ->
                addParticipantVisible(participant)
            }
            Timber.d("Num visible participants is ${visibleParticipants.size}")
        }
    }

    override fun onCleared() {
        super.onCleared()

        LiveEventBus.get(EVENT_UPDATE_CALL, MegaChatCall::class.java)
            .removeObserver(updateCallObserver)

        LiveEventBus.get(EventConstants.EVENT_NOT_OUTGOING_CALL, Long::class.java)
            .removeObserver(noOutgoingCallObserver)

        LiveEventBus.get<Pair<Long, Boolean>>(EventConstants.EVENT_UPDATE_WAITING_FOR_OTHERS)
            .removeObserver(waitingForOthersBannerObserver)
    }

    override fun onEditedChatRoomName(chatId: Long, name: String) {
        if (_state.value.currentChatId == chatId) {
            _state.update { state ->
                state.copy(
                    chatTitle = name,
                )
            }
        }
    }

    /**
     * Determine the chat room has only one moderator and the list is not empty and I am moderator
     *
     * @return True, if you can be assigned as a moderator. False, otherwise.
     */
    private fun shouldAssignModerator(): Boolean {
        if (!isModerator() || numParticipants() == 0) {
            return false
        }

        return participants.value?.toList()?.filter { it.isModerator }.isNullOrEmpty()
    }

    /**
     * Get num of participants in the call
     *
     * @return num of participants
     */
    fun numParticipants(): Int {
        participants.value?.size?.let { numParticipants ->
            return numParticipants
        }

        return 0
    }

    /**
     * Log out of the chat for join as guest action
     */
    fun chatLogout(listener: MegaChatRequestListenerInterface) =
        inMeetingRepository.chatLogout(listener)

    /**
     * Method to create an ephemera plus plus account, required before joining the chat room
     *
     * @param firstName First name of the guest
     * @param lastName Last name of the guest
     * @param listener MegaRequestListenerInterface
     */
    fun createEphemeralAccountAndJoinChat(
        firstName: String,
        lastName: String,
        listener: MegaRequestListenerInterface,
    ) = inMeetingRepository.createEphemeralAccountPlusPlus(firstName, lastName, listener)

    /**
     * Method to open chat preview when joining as a guest
     *
     * @param link The link to the chat room or the meeting
     * @param listener MegaChatRequestListenerInterface
     */
    fun openChatPreview(link: String, listener: MegaChatRequestListenerInterface) =
        inMeetingRepository.openChatPreview(link, listener)

    /**
     * Method to join a chat group
     *
     * @param chatId Chat ID
     * @param listener MegaChatRequestListenerInterface
     */
    fun joinPublicChat(chatId: Long, listener: MegaChatRequestListenerInterface) =
        inMeetingRepository.joinPublicChat(chatId, listener)

    /**
     * Method to rejoin a chat group
     *
     * @param chatId Chat ID
     * @param publicChatHandle MegaChatHandle that corresponds with the public handle of chat room
     * @param listener MegaChatRequestListenerInterface
     */
    fun rejoinPublicChat(
        chatId: Long,
        publicChatHandle: Long,
        listener: MegaChatRequestListenerInterface,
    ) {
        inMeetingRepository.rejoinPublicChat(chatId, publicChatHandle, listener)
    }

    /**
     * Method to add the chat listener when joining as a guest
     *
     * @param chatId Chat ID
     * @param callback
     */
    fun registerConnectionUpdateListener(chatId: Long, callback: () -> Unit) =
        inMeetingRepository.registerConnectionUpdateListener(chatId, callback)

    /**
     * Get my own information
     *
     * @param audio local audio
     * @param video local video
     * @return Me as a Participant
     */
    fun getMyOwnInfo(audio: Boolean, video: Boolean): Participant {
        val participant = inMeetingRepository.getMyInfo(
            getOwnPrivileges() == PRIV_MODERATOR,
            audio,
            video
        )
        participant.hasOptionsAllowed =
            shouldParticipantsOptionBeVisible(participant.isMe, participant.isGuest)

        return participant
    }

    /**
     * Determine if I am a guest
     *
     * @return True, if I am a guest. False if not
     */
    fun amIAGuest(): Boolean = inMeetingRepository.amIAGuest()

    /**
     * Determine if I am a moderator
     *
     * @return True, if I am a moderator. False if not
     */
    fun amIAModerator(): Boolean = getOwnPrivileges() == PRIV_MODERATOR

    /**
     * Determine if the participant has standard privileges
     *
     * @param peerId User handle of a participant
     */
    fun isStandardUser(peerId: Long): Boolean =
        inMeetingRepository.getChatRoom(_state.value.currentChatId)
            ?.let { it.getPeerPrivilegeByHandle(peerId) == MegaChatRoom.PRIV_STANDARD } ?: false

    /**
     * Determine if I am a moderator
     *
     * @return True, if I am a moderator. False, if not
     */
    fun isModerator(): Boolean =
        getOwnPrivileges() == PRIV_MODERATOR

    /**
     * Method to check if tips should be displayed
     *
     * @return True, if tips must be shown. False, if not.
     */
    fun shouldShowTips(): Boolean =
        !MegaApplication.getInstance().applicationContext.defaultSharedPreferences
            .getBoolean(IS_SHOWED_TIPS, false)

    /**
     * Update whether or not to display tips
     */
    fun updateShowTips() {
        MegaApplication.getInstance().applicationContext.defaultSharedPreferences.edit()
            .putBoolean(IS_SHOWED_TIPS, true).apply()
    }

    companion object {
        const val IS_SHOWED_TIPS = "is_showed_meeting_bottom_tips"
    }

    override fun onUserEmailUpdate(email: String?, handler: Long, position: Int) {
        if (email == null)
            return

        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            participants.value = participants.value?.map { participant ->
                return@map when (participant.peerId) {
                    handler -> {
                        participant.copy(isGuest = false)
                    }

                    else -> participant
                }
            }?.toMutableList()
        }
    }

    /**
     * Update the connection status
     *
     * @param status new status
     */
    fun updateNetworkStatus(status: Boolean) =
        _state.update {
            it.copy(
                haveConnection = status,
            )
        }

    /**
     * Method for updating meeting info panel information
     */
    private fun updateMeetingInfoBottomPanel(context: Context) {
        var nameList =
            if (isModerator()) inMeetingRepository.getMyName() else ""
        var numParticipantsModerator = if (isModerator()) 1 else 0
        var numParticipants = 1

        participants.value?.let { list ->
            numParticipants += list.count { !it.isScreenShared }
            list.filter { it.isModerator && it.name.isNotEmpty() }
                .map { it.name }
                .forEach {
                    numParticipantsModerator++
                    nameList = if (nameList.isNotEmpty()) "$nameList, $it" else it
                }
        }

        _state.update { state ->
            state.copy(
                updateNumParticipants = numParticipants,
                updateModeratorsName = if (numParticipantsModerator == 0) "" else
                    context.resources.getQuantityString(
                        R.plurals.meeting_call_screen_meeting_info_bottom_panel_name_of_moderators,
                        numParticipantsModerator,
                        nameList
                    )
            )
        }
    }

    /**
     * Send add contact invitation
     *
     * @param context the Context
     * @param peerId the peerId of users
     * @param callback the callback for sending add contact request
     */
    fun addContact(context: Context, peerId: Long, callback: (String) -> Unit) {
        inMeetingRepository.addContact(context, peerId, callback)
    }

    /**
     * Get avatar from sdk
     *
     * @param peerId the peerId of participant
     */
    fun getRemoteAvatar(peerId: Long) {
        inMeetingRepository.getRemoteAvatar(peerId)
    }

    /**
     * Method for clearing the list of speakers
     */
    fun clearSpeakerParticipants() {
        speakerParticipants.value?.clear()
    }

    /**
     * Method to obtain the current speaker
     *
     * @return The speaker
     */
    fun getCurrentSpeakerParticipant(): Participant? {
        if (speakerParticipants.value.isNullOrEmpty()) {
            return null
        }

        speakerParticipants.value?.let { listSpeakerParticipants ->
            val listFound = listSpeakerParticipants.filter { participant ->
                participant.isSpeaker
            }

            if (listFound.isNotEmpty()) {
                return listFound[0]
            }
        }

        return null
    }

    /**
     * Method to remove the current speaker
     */
    private fun removeCurrentSpeaker() {
        speakerParticipants.value?.let { listSpeakerParticipants ->
            val listFound = listSpeakerParticipants.filter { speaker ->
                speaker.isSpeaker
            }

            if (listFound.isNotEmpty()) {
                val iterator = listFound.iterator()
                iterator.forEach { participant ->
                    if (participant.videoListener != null) {
                        removeResolutionAndListener(
                            participant, participant.videoListener ?: return@forEach
                        )
                        participant.videoListener = null
                    }
                    val position = listSpeakerParticipants.indexOf(participant)
                    if (position != INVALID_POSITION) {
                        speakerParticipants.value?.removeAt(position)
                        Timber.d("Num of speaker participants: ${speakerParticipants.value?.size}")
                        speakerParticipants.value = speakerParticipants.value
                    }
                }
            }
        }
    }

    /**
     * Method to eliminate which are no longer speakers
     */
    fun removePreviousSpeakers() {
        speakerParticipants.value?.let { listSpeakerParticipants ->
            val listFound = listSpeakerParticipants.filter { speaker ->
                !speaker.isSpeaker
            }

            if (listFound.isNotEmpty()) {
                val iterator = listFound.iterator()
                iterator.forEach { participant ->
                    if (participant.videoListener != null) {
                        removeResolutionAndListener(
                            participant,
                            participant.videoListener ?: return@forEach
                        )
                        participant.videoListener = null
                    }
                    val position = listSpeakerParticipants.indexOf(participant)
                    if (position != INVALID_POSITION) {
                        speakerParticipants.value?.removeAt(position)
                        Timber.d("Num of speaker participants: ${speakerParticipants.value?.size}")
                        speakerParticipants.value = speakerParticipants.value
                    }
                }
            }
        }
    }

    /**
     * Method to add a new speaker to the list
     *
     * @param participant The participant who is chosen as speaker
     */
    private fun addSpeaker(participant: Participant) {
        if (speakerParticipants.value.isNullOrEmpty()) {
            createSpeaker(participant)
        } else {
            speakerParticipants.value?.let { listParticipants ->
                val iterator = listParticipants.iterator()
                iterator.forEach { speaker ->
                    speaker.isSpeaker =
                        speaker.peerId == participant.peerId && speaker.clientId == participant.clientId
                }
            }

            speakerParticipants.value?.let { listSpeakerParticipants ->
                val listFound = listSpeakerParticipants.filter { speaker ->
                    speaker.peerId == participant.peerId && speaker.clientId == participant.clientId
                }

                if (listFound.isEmpty()) {
                    createSpeaker(participant)
                }
            }
        }
    }

    /**
     * Method for creating a participant speaker
     *
     * @param participant The participant who is chosen as speaker
     */
    private fun createSpeaker(participant: Participant) {
        createSpeakerParticipant(participant).let { speakerParticipantCreated ->
            speakerParticipants.value?.add(speakerParticipantCreated)
            Timber.d("Num of speaker participants: ${speakerParticipants.value?.size}")
            speakerParticipants.value = speakerParticipants.value
        }
    }

    private fun participantHasScreenSharedParticipant(participant: Participant): Boolean {
        participants.value?.filter { it.peerId == participant.peerId && it.clientId == participant.clientId && it.isScreenShared }
            ?.let {
                return it.isNotEmpty()
            }

        return false
    }

    /**
     * Method to get the list of participants who are no longer speakers
     *
     * @param peerId Peer ID of current speaker
     * @param clientId Client ID of current speaker
     * @return a list of participants who are no longer speakers
     */
    fun getPreviousSpeakers(peerId: Long, clientId: Long): List<Participant>? {
        if (speakerParticipants.value.isNullOrEmpty()) {
            return null
        }

        val checkParticipant = (speakerParticipants.value ?: return null).filter {
            it.peerId != peerId || it.clientId != clientId
        }

        if (checkParticipant.isNotEmpty()) {
            return checkParticipant
        }

        return null
    }

    /**
     * Method to know if local video is activated
     *
     * @return True, if it's on. False, if it's off
     */
    fun isLocalCameraOn(): Boolean = getCall()?.hasLocalVideo() ?: false

    /**
     * Method that controls whether a participant's options (3 dots) should be enabled or not
     *
     * @param participantIsMe If the participant is me
     * @param participantIsGuest If the participant is a guest
     * @return True, if should be enabled. False, if not
     */
    private fun shouldParticipantsOptionBeVisible(
        participantIsMe: Boolean,
        participantIsGuest: Boolean,
    ): Boolean {
        if ((!amIAModerator() && participantIsGuest) ||
            (amIAGuest() && participantIsMe) ||
            (!amIAModerator() && amIAGuest() && !participantIsMe)
        ) {
            return false
        }

        return true
    }

    /**
     * End for all specified call
     *
     * @param chatId Chat ID
     */
    private fun endCallForAll(chatId: Long) {
        Timber.d("End for all. Chat id $chatId")
        endCallUseCase.endCallForAllWithChatId(chatId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onComplete = {
                viewModelScope.launch {
                    broadcastCallEndedUseCase(chatId)
                }
            }, onError = { error ->
                Timber.e(error.stackTraceToString())
            })
            .addTo(composite)
    }

    /**
     * End for all the current call
     */
    fun endCallForAll() {
        callLiveData.value?.let { call ->
            endCallForAll(call.chatid)
        }

        viewModelScope.launch {
            kotlin.runCatching {
                sendStatisticsMeetingsUseCase(EndCallForAll())
            }
        }
    }

    /**
     * Hang up a specified call
     *
     * @param callId Call ID
     */
    private fun hangCall(callId: Long) {
        Timber.d("Hang up call. Call id $callId")
        endCallUseCase.hangCall(callId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onComplete = {
                viewModelScope.launch {
                    broadcastCallEndedUseCase(state.value.currentChatId)
                }
            }, onError = { error ->
                Timber.e(error.stackTraceToString())
            })
            .addTo(composite)
    }

    /**
     * Hang up the current call
     */
    fun hangCall() {
        callLiveData.value?.let { call ->
            hangCall(call.callId)
        }
    }

    /**
     * Control when the hang up button is clicked
     */
    fun checkClickEndButton() {
        if (isOneToOneCall()) {
            hangCall()
            return
        }

        if (amIAGuest()) {
            _callLiveData.value?.let {
                LiveEventBus.get(
                    EventConstants.EVENT_REMOVE_CALL_NOTIFICATION,
                    Long::class.java
                ).post(it.callId)

                hangCall(it.callId)
            }
            return
        }

        if (numParticipants() == 0) {
            hangCall()
            return
        }

        getChat()?.let { chat ->
            when (chat.ownPrivilege) {
                PRIV_MODERATOR -> {
                    _showAssignModeratorBottomPanel.value = false
                    _showEndMeetingAsModeratorBottomPanel.value = true
                }

                else -> hangCall()
            }
        }
    }

    /**
     * Control when the leave button is clicked
     */
    fun checkClickLeaveButton() {
        getChat()?.let { chat ->
            if (chat.isMeeting && shouldAssignModerator()) {
                _showEndMeetingAsModeratorBottomPanel.value = false
                _showAssignModeratorBottomPanel.value = true
            } else {
                hangCall()
            }
        }
    }

    /**
     * Hide bottom panels
     */
    fun hideBottomPanels() {
        _showEndMeetingAsModeratorBottomPanel.value = false
        _showAssignModeratorBottomPanel.value = false
    }

    /**
     * Set call UI status
     *
     * @param newStatus [CallUIStatusType]
     */
    fun setStatus(newStatus: CallUIStatusType) {
        _state.update { it.copy(callUIStatus = newStatus) }
    }

    /**
     * On reject button tapped
     *
     * @param chatId Chat id of the incoming call
     */
    fun onRejectBottomTap(chatId: Long) {
        removeIncomingCallNotification(chatId)
        if (isOneToOneCall()) {
            checkClickEndButton()
        } else {
            ignoreCall()
        }
    }

    /**
     * Sets snackbarMessage in state as consumed.
     */
    fun onSnackbarMessageConsumed() =
        _state.update { state -> state.copy(snackbarMessage = consumed()) }
}