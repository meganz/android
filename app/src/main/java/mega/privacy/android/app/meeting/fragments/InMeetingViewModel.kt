package mega.privacy.android.app.meeting.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.view.TextureView
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx3.asFlowable
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.components.twemoji.EmojiTextView
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.fragments.homepage.Event
import mega.privacy.android.app.listeners.GetUserEmailListener
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.meeting.listeners.GroupVideoListener
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.model.InMeetingUiState
import mega.privacy.android.app.presentation.meeting.model.ParticipantsChange
import mega.privacy.android.app.presentation.meeting.model.ParticipantsChangeType
import mega.privacy.android.app.usecase.call.AmIAloneOnAnyCallUseCase
import mega.privacy.android.app.usecase.call.GetParticipantsChangesUseCase
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.getTitleChat
import mega.privacy.android.app.utils.Constants.AVATAR_CHANGE
import mega.privacy.android.app.utils.Constants.INVALID_POSITION
import mega.privacy.android.app.utils.Constants.INVALID_VALUE
import mega.privacy.android.app.utils.Constants.NAME_CHANGE
import mega.privacy.android.app.utils.Constants.TYPE_JOIN
import mega.privacy.android.app.utils.Constants.TYPE_LEFT
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.call.AnotherCallType
import mega.privacy.android.domain.entity.call.CallCompositionChanges
import mega.privacy.android.domain.entity.call.CallOnHoldType
import mega.privacy.android.domain.entity.call.CallUIStatusType
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatSession
import mega.privacy.android.domain.entity.call.ChatSessionChanges
import mega.privacy.android.domain.entity.call.ChatSessionStatus
import mega.privacy.android.domain.entity.call.ParticipantsCountChange
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.meeting.NetworkQualityType
import mega.privacy.android.domain.entity.meeting.SubtitleCallType
import mega.privacy.android.domain.entity.statistics.EndCallEmptyCall
import mega.privacy.android.domain.entity.statistics.EndCallForAll
import mega.privacy.android.domain.entity.statistics.StayOnCallEmptyCall
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.call.BroadcastCallEndedUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallUseCase
import mega.privacy.android.domain.usecase.call.MonitorParticipatingInAnotherCallUseCase
import mega.privacy.android.domain.usecase.call.SetIgnoredCallUseCase
import mega.privacy.android.domain.usecase.call.StartCallUseCase
import mega.privacy.android.domain.usecase.chat.EndCallUseCase
import mega.privacy.android.domain.usecase.chat.HoldChatCallUseCase
import mega.privacy.android.domain.usecase.chat.IsEphemeralPlusPlusUseCase
import mega.privacy.android.domain.usecase.chat.MonitorCallReconnectingStatusUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRoomUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.MonitorParticipatingInACallInOtherChatsUseCase
import mega.privacy.android.domain.usecase.chat.SetChatTitleUseCase
import mega.privacy.android.domain.usecase.chat.link.JoinPublicChatUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.ChatLogoutUseCase
import mega.privacy.android.domain.usecase.meeting.EnableAudioLevelMonitorUseCase
import mega.privacy.android.domain.usecase.meeting.IsAudioLevelMonitorEnabledUseCase
import mega.privacy.android.domain.usecase.meeting.JoinMeetingAsGuestUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.RequestHighResolutionVideoUseCase
import mega.privacy.android.domain.usecase.meeting.RequestLowResolutionVideoUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.StopHighResolutionVideoUseCase
import mega.privacy.android.domain.usecase.meeting.StopLowResolutionVideoUseCase
import mega.privacy.android.domain.usecase.meeting.raisehandtospeak.IsRaiseToHandSuggestionShownUseCase
import mega.privacy.android.domain.usecase.meeting.raisehandtospeak.LowerHandToStopSpeakUseCase
import mega.privacy.android.domain.usecase.meeting.raisehandtospeak.RaiseHandToSpeakUseCase
import mega.privacy.android.domain.usecase.meeting.raisehandtospeak.SetRaiseToHandSuggestionShownUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaChatRoom.PRIV_MODERATOR
import nz.mega.sdk.MegaChatVideoListenerInterface
import nz.mega.sdk.MegaHandleList
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.DurationUnit

/**
 * InMeetingFragment view model.
 *
 * @property inMeetingRepository                [InMeetingRepository]
 * @property startCallUseCase                   [StartCallUseCase]
 * @property monitorCallReconnectingStatusUseCase       [MonitorCallReconnectingStatusUseCase]
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
 * @property requestHighResolutionVideoUseCase  [RequestHighResolutionVideoUseCase]
 * @property requestLowResolutionVideoUseCase   [RequestLowResolutionVideoUseCase]
 * @property stopHighResolutionVideoUseCase     [StopHighResolutionVideoUseCase]
 * @property stopLowResolutionVideoUseCase      [StopLowResolutionVideoUseCase]
 * @property getChatCallUseCase                 [GetChatCallUseCase]
 * @property getChatRoomUseCase                 [GetChatRoomUseCase]
 * @property monitorChatRoomUpdatesUseCase      [MonitorChatRoomUpdatesUseCase]
 * @property joinPublicChatUseCase              [JoinPublicChatUseCase]
 * @property joinMeetingAsGuestUseCase          [JoinMeetingAsGuestUseCase]
 * @property chatLogoutUseCase                  [ChatLogoutUseCase]
 * @property getFeatureFlagValueUseCase         [GetFeatureFlagValueUseCase]
 * @property raiseHandToSpeakUseCase            [RaiseHandToSpeakUseCase]
 * @property lowerHandToStopSpeakUseCase        [LowerHandToStopSpeakUseCase]
 * @property holdChatCallUseCase                [HoldChatCallUseCase]
 * @property monitorParticipatingInAnotherCallUseCase [MonitorParticipatingInACallInOtherChatsUseCase]
 * @property getStringFromStringResMapper        [GetStringFromStringResMapper]
 * @property isEphemeralPlusPlusUseCase         [IsEphemeralPlusPlusUseCase]
 * @property isRaiseToHandSuggestionShownUseCase [IsRaiseToHandSuggestionShownUseCase]
 * @property state                              Current view state as [InMeetingUiState]
 * @property context                            Application context
 */
@HiltViewModel
@SuppressLint("StaticFieldLeak")
class InMeetingViewModel @Inject constructor(
    private val inMeetingRepository: InMeetingRepository,
    private val startCallUseCase: StartCallUseCase,
    private val monitorCallReconnectingStatusUseCase: MonitorCallReconnectingStatusUseCase,
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
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase,
    private val monitorChatRoomUpdatesUseCase: MonitorChatRoomUpdatesUseCase,
    private val requestHighResolutionVideoUseCase: RequestHighResolutionVideoUseCase,
    private val requestLowResolutionVideoUseCase: RequestLowResolutionVideoUseCase,
    private val stopHighResolutionVideoUseCase: StopHighResolutionVideoUseCase,
    private val stopLowResolutionVideoUseCase: StopLowResolutionVideoUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val broadcastCallEndedUseCase: BroadcastCallEndedUseCase,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val joinMeetingAsGuestUseCase: JoinMeetingAsGuestUseCase,
    private val joinPublicChatUseCase: JoinPublicChatUseCase,
    private val chatLogoutUseCase: ChatLogoutUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val holdChatCallUseCase: HoldChatCallUseCase,
    private val monitorParticipatingInAnotherCallUseCase: MonitorParticipatingInAnotherCallUseCase,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
    private val isEphemeralPlusPlusUseCase: IsEphemeralPlusPlusUseCase,
    private val raiseHandToSpeakUseCase: RaiseHandToSpeakUseCase,
    private val lowerHandToStopSpeakUseCase: LowerHandToStopSpeakUseCase,
    private val isRaiseToHandSuggestionShownUseCase: IsRaiseToHandSuggestionShownUseCase,
    private val setRaiseToHandSuggestionShownUseCase: SetRaiseToHandSuggestionShownUseCase,
    private val setIgnoredCallUseCase: SetIgnoredCallUseCase,
    private val setChatTitleUseCase: SetChatTitleUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
    private val amIAloneOnAnyCallUseCase: AmIAloneOnAnyCallUseCase,
    @ApplicationContext private val context: Context,
) : ViewModel(), GetUserEmailListener.OnUserEmailUpdateCallback {

    private val composite = CompositeDisposable()

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(InMeetingUiState())

    /**
     * public UI State
     */
    val state = _state.asStateFlow()

    private var reconnectingJob: Job? = null

    private var monitorParticipatingInAnotherCallJob: Job? = null
    private var monitorChatRoomUpdatesJob: Job? = null
    private var monitorChatCallUpdatesJob: Job? = null
    private var monitorChatSessionUpdatesJob: Job? = null


    private val _pinItemEvent = MutableLiveData<Event<Participant>>()
    val pinItemEvent: LiveData<Event<Participant>> = _pinItemEvent

    private var meetingLeftTimerJob: Job? = null

    /**
     * Check if is online (connected to Internet)
     *
     * @return True if it is only or False otherwise.
     */
    fun isOnline(): Boolean = isConnectedToInternetUseCase()

    /**
     * Participant selected
     *
     * @param participant [Participant]
     */
    fun onItemClick(participant: Participant) {
        _pinItemEvent.value = Event(participant)
        getSessionByClientId(participant.clientId)?.let {
            if (it.hasScreenShare && _state.value.callUIStatus == CallUIStatusType.SpeakerView) {
                if (!participant.isScreenShared) {
                    triggerSnackbarInSpeakerViewMessage(
                        getStringFromStringResMapper(
                            R.string.meetings_meeting_screen_main_view_participant_is_sharing_screen_warning
                        )
                    )
                }

                Timber.d("Participant clicked: $participant")
                sortParticipantsListForSpeakerView(participant)
            }
        }
    }

    /**
     * Pin speaker and sort the participants list
     *
     * @param participant   [Participant]
     */
    private fun pinSpeaker(participant: Participant) {
        _pinItemEvent.value = Event(participant)
        sortParticipantsListForSpeakerView(participant)
    }

    /**
     * Chat participant select to be in speaker view
     *
     * @param chatParticipant [ChatParticipant]
     */
    fun onItemClick(chatParticipant: ChatParticipant) =
        participants.value?.find { it.peerId == chatParticipant.handle }?.let {
            onItemClick(it)
        }

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
        getMyUserHandle()

        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.RaiseToSpeak).let { flag ->
                    _state.update { state ->
                        state.copy(
                            isRaiseToSpeakFeatureFlagEnabled = flag,
                        )
                    }
                }
            }.onFailure {
                Timber.e(it)
            }
        }

        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(AppFeatures.PictureInPicture).let { flag ->
                    _state.update { state ->
                        state.copy(
                            isPictureInPictureFeatureFlagEnabled = flag,
                        )
                    }
                }
            }.onFailure {
                Timber.e(it)
            }
        }

        getParticipantsChangesUseCase.getChangesFromParticipants()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { (chatId, typeChange, peers) ->
                    if (_state.value.currentChatId == chatId) {
                        state.value.chat?.let { chat ->
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

        amIAloneOnAnyCallUseCase()
            .asFlowable(viewModelScope.coroutineContext)
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

        LiveEventBus.get<Pair<Long, Boolean>>(EventConstants.EVENT_UPDATE_WAITING_FOR_OTHERS)
            .observeForever(waitingForOthersBannerObserver)
    }


    /**
     * load my user handle and save to ui state
     */
    private fun getMyUserHandle() {
        state.value.myUserHandle?.let { myHandle ->
            if (myHandle != -1L && myHandle != 0L) {
                return
            }
        }

        viewModelScope.launch {
            runCatching {
                val myUserHandle = getMyUserHandleUseCase()
                _state.update { state -> state.copy(myUserHandle = myUserHandle) }
            }.onFailure { Timber.e(it) }
        }
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
                            chat = chat,
                            isMeeting = chat.isMeeting,
                            isPublicChat = chat.isPublic,
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
    private fun getChatCall() {
        viewModelScope.launch {
            runCatching {
                getChatCallUseCase(_state.value.currentChatId)
            }.onSuccess { chatCall ->
                chatCall?.let { call ->
                    Timber.d("Call id ${call.callId} and chat id ${call.chatId} and call users limit ${call.callUsersLimit}")
                    Timber.d("Call limit ${call.callDurationLimit} Call Duration ${call.duration} and initial timestamp ${call.initialTimestamp}")
                    Timber.d("Call user limit ${call.callUsersLimit} and users in call ${call.peerIdParticipants?.size}")

                    _state.update { it.copy(call = call) }
                    checkSubtitleToolbar()

                    call.status?.let { status ->
                        if (_updateCallId.value != call.callId) {
                            _updateCallId.value = call.callId
                            checkParticipantsList()
                            checkReconnectingChanges()
                            updateMeetingInfoBottomPanel()
                        }
                        if (status != ChatCallStatus.Initial && _state.value.previousState == ChatCallStatus.Initial) {
                            _state.update { it.copy(previousState = status) }
                        }
                        handleFreeCallEndWarning()
                        isEphemeralAccount()
                        getMyUserHandle()
                        if (status == ChatCallStatus.InProgress && state.value.isRaiseToSpeakFeatureFlagEnabled && isOneToOneCall().not()) {
                            Timber.d("Call recovered, check the participants with raised  hand")
                            updateParticipantsWithRaisedHand(call)
                        }

                        updateNetworkQuality(call.networkQuality)
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Get another chat call
     *
     * @param chatId    Chat id
     */
    private fun getAnotherChatCall(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                getChatCallUseCase(chatId)
            }.onSuccess { chatCall ->
                chatCall?.let { call ->
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

                    inMeetingRepository.getChatRoom(call.chatId)?.let { chat ->
                        _state.update { state ->
                            state.copy(
                                anotherChatTitle = getTitleChat(chat),
                            )
                        }
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Update network quality
     *
     * @param quality   [NetworkQualityType]
     */
    private fun updateNetworkQuality(quality: NetworkQualityType?) {
        _showPoorConnectionBanner.value = quality == NetworkQualityType.Bad
    }


    /**
     * Get chat room updates
     */
    private fun startMonitorChatRoomUpdates() {
        monitorChatRoomUpdatesJob?.cancel()
        monitorChatRoomUpdatesJob = viewModelScope.launch {
            monitorChatRoomUpdatesUseCase(_state.value.currentChatId).collectLatest { chat ->
                _state.update { it.copy(chat = chat) }

                if (chat.hasChanged(ChatRoomChange.Title)) {
                    Timber.d("Changes in chat title")
                    _state.update { state ->
                        state.copy(
                            chatTitle = chat.title,
                        )
                    }
                }
                if (chat.hasChanged(ChatRoomChange.OpenInvite)) {
                    _state.update { it.copy(isOpenInvite = chat.isOpenInvite) }
                }
            }
        }
    }

    /**
     * Get chat call updates
     */
    private fun startMonitorChatSessionUpdates() {
        monitorChatSessionUpdatesJob?.cancel()
        monitorChatSessionUpdatesJob = viewModelScope.launch {
            monitorChatSessionUpdatesUseCase()
                .filter { it.call?.chatId == _state.value.currentChatId }
                .collectLatest { result ->
                    result.call?.let { call ->
                        _state.update { it.copy(call = call) }
                        result.session?.let { session ->
                            session.changes?.apply {
                                Timber.d("Changes in session: $this")

                                when {
                                    contains(ChatSessionChanges.Status) -> _state.update {
                                        it.copy(
                                            changesInStatusInSession = session
                                        )
                                    }

                                    contains(ChatSessionChanges.SessionOnHold) -> _state.update {
                                        it.copy(
                                            sessionOnHoldChanges = session
                                        )
                                    }

                                    contains(ChatSessionChanges.RemoteAvFlags) -> _state.update {
                                        it.copy(
                                            changesInAVFlagsInSession = session
                                        )
                                    }

                                    contains(ChatSessionChanges.AudioLevel) -> _state.update {
                                        it.copy(
                                            changesInAudioLevelInSession = session
                                        )
                                    }

                                    contains(ChatSessionChanges.SessionOnHiRes) -> _state.update {
                                        it.copy(
                                            changesInHiResInSession = session
                                        )
                                    }

                                    contains(ChatSessionChanges.SessionOnLowRes) -> _state.update {
                                        it.copy(
                                            changesInLowResInSession = session
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    /**
     * Get chat call updates
     */
    private fun startMonitorChatCallUpdates() {
        monitorChatCallUpdatesJob?.cancel()
        monitorChatCallUpdatesJob = viewModelScope.launch {
            monitorChatCallUpdatesUseCase()
                .filter { it.chatId == _state.value.currentChatId }
                .collectLatest { call ->
                    _state.update { it.copy(call = call) }
                    Timber.d("Call user limit ${call.callUsersLimit} and users in call ${call.peerIdParticipants?.size}")
                    checkSubtitleToolbar()
                    call.changes?.apply {
                        Timber.d("Changes in call: $this")

                        when {
                            contains(ChatCallChanges.Status) -> {
                                Timber.d("Call status changed ${call.status}")
                                call.status?.let { status ->
                                    if (status == ChatCallStatus.InProgress) {
                                        Timber.d("Call in progress, get my user information")
                                        isEphemeralAccount()
                                        getMyUserHandle()
                                        if (state.value.isRaiseToSpeakFeatureFlagEnabled && isOneToOneCall().not()) {
                                            Timber.d("Call in progress, check the participants with raised hand")
                                            updateParticipantsWithRaisedHand(call)
                                        }
                                    }
                                    checkSubtitleToolbar()
                                    _state.update { state ->
                                        state.copy(
                                            previousState = status,
                                        )
                                    }
                                }
                            }

                            contains(ChatCallChanges.CallWillEnd) -> handleFreeCallEndWarning()
                            contains(ChatCallChanges.CallRaiseHand) -> if (state.value.isRaiseToSpeakFeatureFlagEnabled && isOneToOneCall().not()) {
                                Timber.d("Change in CallRaiseHand, update participants with raised hand")
                                updateParticipantsWithRaisedHand(call)
                            }

                            contains(ChatCallChanges.LocalAVFlags) -> checkUpdatesInLocalAVFlags(
                                update = true
                            )

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

                                    if (showReconnectingBanner.value || !isOnline()) {
                                        Timber.d("Back from reconnecting")
                                    } else {
                                        Timber.d("Change in call composition, review the UI")
                                        if (isOneToOneCall()) {
                                            if (call.numParticipants == 1 || call.numParticipants == 2) {
                                                checkUpdatesInCallComposition(update = true)
                                            }
                                        } else {
                                            checkUpdatesInCallComposition(update = true)
                                        }
                                    }
                                }
                            }

                            contains(ChatCallChanges.NetworkQuality) -> updateNetworkQuality(call.networkQuality)
                        }
                    }
                }
        }
    }

    /**
     * Check update Local AVFlags
     *
     * @param update    True, is updated. False, if not.
     */
    fun checkUpdatesInLocalAVFlags(update: Boolean) = _state.update { state ->
        state.copy(
            shouldUpdateLocalAVFlags = update,
        )
    }

    /**
     * Check update in call composition
     *
     * @param update    True, is updated. False, if not.
     */
    fun checkUpdatesInCallComposition(update: Boolean) = _state.update { state ->
        state.copy(
            shouldCheckChildFragments = update,
        )
    }

    /**
     * Update participants with hand raised list
     *
     * @param call  [ChatCall]
     */
    private fun updateParticipantsWithRaisedHand(call: ChatCall) {
        val listWithChanges = buildList {
            participants.value = participants.value?.map { participant ->
                call.usersRaiseHands[participant.peerId]?.let { isRaisedHand ->
                    // update the participant's isRaisedHand status and order based on the corresponding values in the usersRaiseHands.
                    // If the participant's isRaisedHand status changes, their ID is added to the listWithChanges.
                    if (participant.isRaisedHand != isRaisedHand) {
                        add(participant.peerId)
                        participant.copy(isRaisedHand = isRaisedHand)
                    } else {
                        participant
                    }
                } ?: if (participant.isRaisedHand) {
                    add(participant.peerId)
                    // If a participant's ID is not found in the triple list, it means they have not raised their hand.
                    // In this case, their isRaisedHand status is set to false
                    participant.copy(isRaisedHand = false)
                } else {
                    participant
                }
            }?.toMutableList()
        }
        _state.update { state -> state.copy(userIdsWithChangesInRaisedHand = listWithChanges) }
    }

    /**
     * clear user ids with changes in raised hand list
     */
    fun cleanParticipantsWithRaisedOrLoweredHandsChanges() =
        _state.update { state -> state.copy(userIdsWithChangesInRaisedHand = emptyList()) }

    /**
     * Check if exists another call on hold
     */
    private fun startMonitorParticipatingInACall(chatId: Long) {
        monitorParticipatingInAnotherCallJob?.cancel()
        monitorParticipatingInAnotherCallJob = viewModelScope.launch {
            monitorParticipatingInAnotherCallUseCase(chatId)
                .catch { Timber.e(it) }
                .collect {
                    val currentCall = it.firstOrNull()
                    _state.update { state -> state.copy(anotherCall = currentCall) }
                    if (currentCall != null) {
                        getAnotherChatCall(currentCall.chatId)
                    } else {
                        _state.update { state ->
                            state.copy(
                                updateAnotherCallBannerType = AnotherCallType.NotCall,
                            )
                        }
                    }
                }
        }
    }

    /**
     * Cancel the timer when call is upgraded or start the end timer if the call is free
     */
    private fun handleFreeCallEndWarning() {
        val call = _state.value.call ?: return
        if (call.callWillEndTs == null || call.callWillEndTs == 0L) return
        if (call.callWillEndTs == -1L) {
            Timber.d("Cancelling Meeting Timer Job")
            // CALL_LIMIT_DISABLED
            meetingLeftTimerJob?.cancel()
            _state.update {
                it.copy(
                    minutesToEndMeeting = null,
                    showMeetingEndWarningDialog = false
                )
            }

        } else {
            Timber.d("Call will end in ${call.callWillEndTs} seconds")
            call.callWillEndTs?.let { timeStampInSecond ->
                val currentTimeStampInSecond =
                    TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis())
                val seconds = timeStampInSecond - currentTimeStampInSecond
                val minutes = TimeUnit.SECONDS.toMinutes(seconds)
                Timber.d("Call will end in $minutes minutes")
                startMeetingEndWarningTimer(if (minutes == 0L) 1 else minutes.toInt())
            }
        }
    }

    private fun startMeetingEndWarningTimer(minutes: Int) {
        meetingLeftTimerJob?.cancel()
        meetingLeftTimerJob = viewModelScope.launch {
            (minutes downTo 0).forEach { minute ->
                Timber.d("Meeting will end in $minute minutes")
                _state.update {
                    it.copy(minutesToEndMeeting = minute)
                }
                delay(TimeUnit.MINUTES.toMillis(1))
            }
            _state.update {
                it.copy(minutesToEndMeeting = null)
            }
            meetingLeftTimerJob = null
        }
    }

    /**
     * set showMeetingEndWarningDialog to false when dialog is shown
     */
    fun onMeetingEndWarningDialogDismissed() {
        _state.update {
            it.copy(showMeetingEndWarningDialog = false)
        }
    }

    /**
     * Show meeting info dialog
     *
     * @param shouldShowDialog True,show dialog.
     */
    fun onToolbarTap(shouldShowDialog: Boolean) =
        _state.update {
            it.copy(showMeetingInfoFragment = isOneToOneCall().not() && shouldShowDialog)
        }

    /**
     * Method to check if only me dialog and the call will end banner should be displayed.
     */
    fun checkShowOnlyMeBanner() {
        if (isOneToOneCall())
            return

        state.value.call?.let { call ->
            checkIfIAmTheOnlyOneOnTheCall(call).run {
                if (onlyMeInTheCall) {
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
     * Method to check if I am alone on the call and whether it is because I am waiting for others or because everyone has dropped out of the call
     *
     * @param call [ChatCall]
     * @return NumParticipantsChangesResult
     */
    private fun checkIfIAmTheOnlyOneOnTheCall(call: ChatCall): ParticipantsCountChange {
        var waitingForOthers = false
        var onlyMeInTheCall = false
        if (isOneToOneCall().not()) {
            call.peerIdParticipants?.let { list ->
                onlyMeInTheCall =
                    list.size == 1 && list.first() == state.value.myUserHandle

                waitingForOthers = onlyMeInTheCall &&
                        MegaApplication.getChatManagement().isRequestSent(call.callId)
            }
        }

        return ParticipantsCountChange(
            call.chatId,
            onlyMeInTheCall,
            waitingForOthers,
            isReceivedChange = true
        )
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
                    getParticipantName(list.first())
                )
            }

            2 -> { context: Context ->
                context.getString(
                    if (type == TYPE_JOIN)
                        R.string.meeting_call_screen_two_participants_joined_call
                    else
                        R.string.meeting_call_screen_two_participants_left_call,
                    getParticipantName(list.first()), getParticipantName(list.last())
                )
            }

            else -> { context: Context ->
                context.resources.getQuantityString(
                    if (type == TYPE_JOIN) R.plurals.meeting_call_screen_more_than_two_participants_joined_call
                    else
                        R.plurals.meeting_call_screen_more_than_two_participants_left_call,
                    numParticipants, getParticipantName(list.first()), (numParticipants - 1)
                )
            }
        }

        _getParticipantsChanges.value = Pair(first = type, second = action)
    }

    /**
     * on consume participant changes
     */
    fun onConsumeParticipantChanges() {
        _getParticipantsChanges.update {
            it.copy(second = null)
        }
        _state.update {
            it.copy(participantsChanges = null)
        }
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
    fun getCallDuration(): Long =
        getCall()?.duration?.toLong(DurationUnit.SECONDS) ?: INVALID_VALUE.toLong()

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
        hangCurrentCall()

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
     * Method that controls whether to display the reconnecting banner
     */
    private fun checkReconnectingChanges() {
        reconnectingJob?.cancel()
        reconnectingJob = viewModelScope.launch {
            monitorCallReconnectingStatusUseCase(_state.value.currentChatId)
                .catch { Timber.e(it) }
                .collectLatest {
                    _showReconnectingBanner.value = it
                }
        }
    }

    /**
     * Method to know if it is the same chat
     *
     * @param chatId chat ID
     * @return True, if it is the same. False, otherwise
     */
    fun isSameChatRoom(chatId: Long): Boolean =
        chatId != MEGACHAT_INVALID_HANDLE && _state.value.currentChatId == chatId

    /**
     * Method to set a call
     *
     * @param chatId chat ID
     */
    fun setCall(chatId: Long) {
        if (isSameChatRoom(chatId)) {
            getChatRoom()
            getChatCall()
        }
    }

    /**
     * Method to get a call
     *
     * @return [ChatCall]
     */
    fun getCall(): ChatCall? = state.value.call

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
     * Method to set a chat
     *
     * @param newChatId chat ID
     */
    fun setChatId(newChatId: Long) {
        if (newChatId == MEGACHAT_INVALID_HANDLE || _state.value.currentChatId == newChatId)
            return

        _state.update { state ->
            state.copy(
                currentChatId = newChatId,
            )
        }

        getChatRoom()
        getChatCall()
        startMonitorParticipatingInACall(_state.value.currentChatId)
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
    fun isOneToOneCall(): Boolean = state.value.isOneToOneCall

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
     * Get session of a participant in one to one call
     *
     */
    fun getSessionOneToOneCall(): ChatSession? = state.value.getSession

    /**
     * Get the [ChatSession] of a participant
     *
     * @param clientId client ID of a participant
     * @return ChatSession of a participant
     */
    fun getSessionByClientId(clientId: Long): ChatSession? =
        state.value.getSessionByClientId(clientId)

    /**
     * Method to know if the session of a participants is null
     *
     * @param clientId The client ID of a participant
     */
    fun isSessionOnHoldByClientId(clientId: Long): Boolean =
        state.value.isSessionOnHoldByClientId(clientId) ?: false

    /**
     * Method to know if a one-to-one call is audio only
     *
     * @return True, if it's audio call. False, otherwise
     */
    fun isAudioCall(): Boolean {
        state.value.call?.let { call ->
            if (call.isOnHold) {
                return true
            }

            getSessionOneToOneCall()?.let { chatSession ->
                if (chatSession.isOnHold || (!call.hasLocalVideo && !MegaApplication.getChatManagement()
                        .getVideoStatus(call.chatId) && !chatSession.hasVideo)
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
        state.value.call?.let { (it.status == ChatCallStatus.InProgress) }
            ?: run { false }

    /**
     * Method to know if a call is on hold
     *
     * @return True, if is on hold. False, otherwise
     */
    fun isCallOnHold(): Boolean = state.value.isCallOnHold ?: false

    /**
     * Method to know if a call or session is on hold in meeting
     *
     * @return True, if is on hold. False, otherwise
     */
    fun isCallOrSessionOnHold(clientId: Long): Boolean =
        (isCallOnHold()) || isSessionOnHoldByClientId(clientId)

    /**
     * Method to know if a call or session is on hold in one to one call
     *
     * @return True, if is on hold. False, otherwise
     */
    fun isCallOrSessionOnHoldOfOneToOneCall(): Boolean =
        if (isCallOnHold()) true else isSessionOnHoldOfOneToOneCall()

    /**
     * Control when join a meeting as a guest
     *
     * @param meetingLink   Meeting link
     * @param firstName     Guest first name
     * @param lastName      Guest last name
     */
    fun joinMeetingAsGuest(meetingLink: String, firstName: String, lastName: String) {
        viewModelScope.launch {
            runCatching {
                joinMeetingAsGuestUseCase(meetingLink, firstName, lastName)
            }.onSuccess {
                chatManagement
                    .setOpeningMeetingLink(
                        state.value.currentChatId,
                        true
                    )
                autoJoinPublicChat()

            }.onFailure { exception ->
                Timber.e(exception)
                chatLogout()
            }
        }
    }

    /**
     * Chat logout
     */
    private fun chatLogout() {
        viewModelScope.launch {
            runCatching {
                chatLogoutUseCase()
            }.onSuccess {
                _state.update {
                    it.copy(
                        shouldFinish = true,
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception)
                _state.update {
                    it.copy(
                        shouldFinish = true,
                    )
                }
            }
        }
    }

    /**
     * Auto join public chat
     */
    private fun autoJoinPublicChat() {
        if (!chatManagement.isAlreadyJoining(state.value.currentChatId)) {
            chatManagement.addJoiningChatId(state.value.currentChatId)
            viewModelScope.launch {
                runCatching {
                    joinPublicChatUseCase(state.value.currentChatId)
                }.onSuccess {
                    chatManagement.removeJoiningChatId(state.value.currentChatId)
                    chatManagement.broadcastJoinedSuccessfully()
                    _state.update {
                        it.copy(
                            joinedAsGuest = true,
                        )
                    }
                }.onFailure { exception ->
                    Timber.e(exception)
                    chatManagement.removeJoiningChatId(state.value.currentChatId)
                    _state.update {
                        it.copy(
                            shouldFinish = true,
                        )
                    }
                }
            }
        }
    }

    /**
     * Sets joinedAsGuest in state as consumed.
     */
    fun onJoinedAsGuestConsumed() = _state.update {
        it.copy(
            joinedAsGuest = false,
        )
    }

    /**
     * Method to know if a session is on hold in one to one call
     *
     * @return True, if is on hold. False, otherwise
     */
    private fun isSessionOnHoldOfOneToOneCall(): Boolean {
        state.value.call?.let { _ ->
            if (isOneToOneCall()) {
                getSessionOneToOneCall()?.let {
                    return it.isOnHold
                }
            }
        }

        return false
    }

    /**
     * Method to obtain the full name of a participant
     *
     * @param peerId User handle of a participant
     * @return The name of a participant
     */
    fun getParticipantFullName(peerId: Long): String? =
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
    ): MutableSet<Participant> {
        val listWithChanges = mutableSetOf<Participant>()
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            participants.value = participants.value?.map { participant ->
                return@map when {
                    participant.peerId == peerId && typeChange == NAME_CHANGE -> {
                        listWithChanges.add(participant)
                        val newName = getParticipantFullName(peerId)
                        participant.copy(
                            name = newName ?: participant.name,
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
            updateMeetingInfoBottomPanel()
        }
        return listWithChanges
    }

    /**
     * Method that makes the necessary changes to the participant list when my own privileges have changed.
     */
    fun updateOwnPrivileges() {
        inMeetingRepository.getChatRoom(_state.value.currentChatId)?.let {
            participants.value = participants.value?.map { participant ->
                return@map participant.copy(
                    hasOptionsAllowed = shouldParticipantsOptionBeVisible(
                        participant.isMe,
                        participant.isGuest
                    )
                )
            }?.toMutableList()
            updateMeetingInfoBottomPanel()
        }
    }

    /**
     * Method for updating participant privileges
     *
     * @return list of participants with changes
     */
    fun updateParticipantsPrivileges(): MutableSet<Participant> {
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
            updateMeetingInfoBottomPanel()
        }

        return listWithChanges
    }


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
        state.value.call?.run {
            if (isOneToOneCall()) {
                getSessionOneToOneCall()?.let { session ->
                    if (!session.hasAudio && session.peerId != MEGACHAT_INVALID_HANDLE) {
                        bannerIcon?.let {
                            it.isVisible = true
                        }
                        bannerText?.let {
                            it.text = it.context.getString(
                                R.string.muted_contact_micro,
                                inMeetingRepository.getContactOneToOneCallName(
                                    session.peerId
                                )
                            )
                        }
                        return true
                    }
                }
            }

            if (!state.value.hasLocalAudio) {
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
        val callId = state.value.call?.callId ?: return false

        return callId != MEGACHAT_INVALID_HANDLE && MegaApplication.getChatManagement()
            .isRequestSent(callId)
    }

    /**
     * Method for determining whether to display the camera switching icon.
     *
     * @return True, if it is. False, if not.
     */
    fun isNecessaryToShowSwapCameraOption(): Boolean =
        state.value.call?.let { it.status != ChatCallStatus.Connecting && it.hasLocalVideo && !it.isOnHold }
            ?: run { false }


    /**
     * Get my own privileges in the chat
     *
     * @return the privileges
     */
    fun getOwnPrivileges(): Int =
        inMeetingRepository.getOwnPrivileges(_state.value.currentChatId)

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
                ?: run { false }

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
                it.isSpeaker && !it.isScreenShared && (it.peerId != newSpeakerPeerId || it.clientId != newSpeakerClientId) -> {
                    Timber.d("The previous speaker ${it.clientId}, now has isSpeaker false")
                    it.isSpeaker = false
                    listWithChanges.add(it)
                }

                !it.isSpeaker && !it.isScreenShared && it.peerId == newSpeakerPeerId && it.clientId == newSpeakerClientId -> {
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
                getSessionByClientId(it.clientId)?.apply {
                    if (hasScreenShare && !participantHasScreenSharedParticipant(it)) {
                        addScreensSharedParticipantsList.add(it)
                    }
                }
            }

            _state.update { state -> state.copy(addScreensSharedParticipantsList = addScreensSharedParticipantsList.toMutableList()) }
        }

        val removeScreensSharedParticipantsList = mutableSetOf<Participant>()
        participants.value?.forEach {
            getSessionByClientId(it.clientId)?.apply {
                if (participantHasScreenSharedParticipant(it) && ((it.isSpeaker && hasScreenShare) || (!it.isSpeaker && !hasScreenShare))) {
                    getScreenShared(it.peerId, it.clientId)?.let { screenShared ->
                        removeScreensSharedParticipantsList.add(screenShared)
                    }
                }
            }
        }

        _state.update { state -> state.copy(removeScreensSharedParticipantsList = removeScreensSharedParticipantsList.toMutableList()) }
    }

    /**
     * Remove screen shared participant
     *
     * @param list  List of [Participant]
     */
    fun removeScreenShareParticipant(list: List<Participant>?): Int {
        _state.update { state -> state.copy(removeScreensSharedParticipantsList = null) }
        list?.forEach { user ->
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
                    updateParticipantsList()
                    return position
                }
            }
        }
        return INVALID_POSITION
    }

    /**
     * Add screen shared participant
     *
     * @param list  List of [Participant]
     * @return  Position of the screen shared
     */
    fun addScreenShareParticipant(list: List<Participant>?): Int? {
        _state.update { state -> state.copy(addScreensSharedParticipantsList = null) }
        list?.forEach { participant ->
            getSessionByClientId(participant.clientId)?.let { session ->
                createParticipant(
                    isScreenShared = true,
                    session
                )?.let { screenSharedParticipant ->
                    participants.value?.indexOf(participant)?.let { index ->
                        participants.value?.add(index, screenSharedParticipant)
                        updateParticipantsList()
                        return participants.value?.indexOf(screenSharedParticipant)
                    }
                }
            }
        }
        return INVALID_POSITION
    }

    /**
     * Method for create a participant
     *
     * @param isScreenShared    True if it's the screen shared. False if not.
     * @param session           [ChatSession]
     * @return [Participant]
     */
    private fun createParticipant(isScreenShared: Boolean, session: ChatSession): Participant? {
        _state.value.call?.apply {
            when {
                isScreenShared ->
                    participants.value?.filter { it.peerId == session.peerId && it.clientId == session.clientId && it.isScreenShared }
                        ?.apply {
                            if (isNotEmpty()) {
                                return null
                            }
                        }

                else ->
                    participants.value?.filter { it.peerId == session.peerId && it.clientId == session.clientId }
                        ?.apply {
                            if (isNotEmpty()) {
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
            } ?: run { false }

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
                isScreenShared = isScreenShared,
                isCameraOn = session.hasCamera,
                isScreenShareOn = session.hasScreenShare,
            )
        }

        return null
    }

    /**
     * Method that creates the participant speaker
     *
     * @param participant The participant who is to be a speaker
     * @return speaker participant
     */
    private fun createSpeakerParticipant(participant: Participant): Participant =
        Participant(
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
            isScreenShared = false,
        )

    /**
     * Method to update the current participants list
     */
    fun checkParticipantsList() {
        state.value.call?.apply {
            val numberOfParticipants: Int = participants.value?.size ?: 0
            sessionsClientId?.let { list ->
                if (list.isNotEmpty() && numberOfParticipants != list.size) {
                    createCurrentParticipants(list)
                }
            }
        }
    }

    /**
     * Method for creating participants already on the call
     *
     * @param list list of participants
     */
    private fun createCurrentParticipants(list: List<Long>?) {
        list?.let { listParticipants ->
            participants.value?.clear()
            if (listParticipants.isNotEmpty()) {
                for (clientId in list) {
                    getSessionByClientId(clientId)?.let { session ->
                        createParticipant(
                            isScreenShared = false,
                            session
                        )?.let { participantCreated ->
                            participants.value?.add(participantCreated)
                        }
                    }
                }

                updateParticipantsList()
            }
        }
    }

    /**
     * Method to control when the number of participants changes
     */
    private fun updateParticipantsList() {
        when (_state.value.callUIStatus) {
            CallUIStatusType.SpeakerView -> sortParticipantsListForSpeakerView()
            else -> participants.value = participants.value
        }
        updateMeetingInfoBottomPanel()
    }

    /**
     * Method for adding a participant to the list
     *
     * @param session  [ChatSession]
     * @return the position of the participant
     */
    fun addParticipant(session: ChatSession): Int? {
        createParticipant(isScreenShared = false, session)?.let { participantCreated ->
            participants.value?.add(participantCreated)
            updateParticipantsList()
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
     * @return the position of the participant
     */
    fun removeParticipant(session: ChatSession): Int {
        participants.value?.first { it.peerId == session.peerId && it.clientId == session.clientId }
            ?.let { participant ->
                val position = participants.value?.indexOf(participant)
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
                    Timber.d("Removing participant... ${session.clientId}")
                    updateParticipantsList()

                    if (isSpeaker) {
                        Timber.d("The removed participant was speaker, clientID ${participant.clientId}")
                        removePreviousSpeakers()
                        removeCurrentSpeaker()
                    }
                    return position
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

        getSessionByClientId(participant.clientId)?.let {
            when {
                participant.hasHiRes && it.canReceiveVideoHiRes -> {
                    Timber.d("Stop HiResolution and remove listener, clientId = ${participant.clientId}")
                    stopHiResVideo(it, _state.value.currentChatId)
                }

                !participant.hasHiRes && it.canReceiveVideoLowRes -> {
                    Timber.d("Stop LowResolution and remove listener, clientId = ${participant.clientId}")
                    stopLowResVideo(it, _state.value.currentChatId)
                }

                else -> {}
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
            participant.isMe,
            participant.isScreenShared
        )
    }

    /**
     * Method for know if the resolution of a participant's video should be high
     *
     * @return True, if should be high. False, otherwise
     */
    private fun needHiRes(): Boolean =
        participants.value?.let { state.value.callUIStatus != CallUIStatusType.SpeakerView }
            ?: run { false }

    /**
     * Method to know if the session has video on and is not on hold
     *
     * @param clientId Client ID of participant
     * @return True, it does. False, if not.
     */
    fun sessionHasVideo(clientId: Long): Boolean =
        getSessionByClientId(clientId)?.let { it.hasVideo && !isCallOrSessionOnHold(it.clientId) && it.status == ChatSessionStatus.Progress }
            ?: run { false }

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
        participants.value?.let { participantsList ->
            Timber.d("Current Participant list $participantsList")
            if (participantsList.isEmpty()) return null

            when {
                peerId == -1L && clientId == -1L -> {
                    participants.value?.sortByDescending { it.isPresenting }
                    return participants.value?.first()
                }

                else -> participantsList.filter { it.peerId != peerId || it.clientId != clientId }
                    .forEach {
                        return it
                    }
            }
        }
        return null
    }

    /**
     * Method to get the first participant in the list, who will be the new speaker
     */
    fun getOwnParticipant(): Participant? {
        return participants.value?.firstOrNull { it.isMe }
    }

    /**
     * Get participant from peerId and clientId
     *
     * @param peerId peer ID of a participant
     * @param clientId client ID of a participant
     */
    fun getParticipant(peerId: Long, clientId: Long): Participant? {
        participants.value?.filter { it.peerId == peerId && it.clientId == clientId && !it.isScreenShared }
            ?.apply {
                return if (isNotEmpty()) first() else null
            }

        return null
    }

    /**
     * Get list of participants with same peer ID
     *
     * @param peerId    Peer ID
     * @return list of [Participant]
     */
    fun getParticipants(peerId: Long): List<Participant>? =
        participants.value?.filter { it.peerId == peerId }

    /**
     * Get participant or screen share from peerId and clientId
     *
     * @param peerId peer ID of a participant
     * @param clientId client ID of a participant
     * @param isScreenShared True, it's the screen shared. False if not.
     * @return The participant or screen shared from peerId and clientId.
     */
    fun getParticipantOrScreenShared(
        peerId: Long,
        clientId: Long,
        isScreenShared: Boolean? = false,
    ): Participant? {
        participants.value?.filter { it.peerId == peerId && it.clientId == clientId && isScreenShared == it.isScreenShared }
            ?.apply {
                return if (isNotEmpty()) first() else null
            }

        return null
    }

    /**
     * Get speaker participant
     *
     * @param peerId    Peer Id
     * @param clientId  Client Id
     * @return [Participant]
     */
    fun getSpeaker(peerId: Long, clientId: Long): Participant? {
        speakerParticipants.value?.filter { it.peerId == peerId && it.clientId == clientId }
            ?.apply {
                return if (isNotEmpty()) first() else null
            }

        return null
    }

    /**
     * Get screen shared participant
     *
     * @param peerId    Peer Id
     * @param clientId  Client Id
     * @return [Participant]
     */
    fun getScreenShared(peerId: Long, clientId: Long): Participant? {
        participants.value?.filter { it.peerId == peerId && it.clientId == clientId && it.isScreenShared }
            ?.apply {
                return if (isNotEmpty()) first() else null
            }

        return null
    }

    /**
     * Method for updating participant video
     *
     * @param session [ChatSession] of a participant
     * @return True, if there have been changes. False, otherwise
     */
    fun changesInRemoteVideoFlag(session: ChatSession): Boolean {
        var hasChanged = false
        participants.value = participants.value?.map { participant ->
            return@map when {
                participant.peerId == session.peerId && participant.clientId == session.clientId -> {
                    if (participant.isVideoOn != session.hasVideo ||
                        participant.isCameraOn != session.hasCamera ||
                        participant.isScreenShareOn != session.hasScreenShare
                    ) {
                        hasChanged = true
                    }

                    return@map participant.copy(
                        isVideoOn = session.hasVideo,
                        isCameraOn = session.hasCamera,
                        isScreenShareOn = session.hasScreenShare
                    )
                }

                else -> participant
            }
        }?.toMutableList()

        speakerParticipants.value = speakerParticipants.value?.map { participant ->
            return@map when {
                participant.peerId == session.peerId && participant.clientId == session.clientId && participant.isVideoOn != session.hasVideo -> {
                    hasChanged = true
                    participant.copy(isVideoOn = session.hasVideo)
                }

                else -> participant
            }
        }?.toMutableList()

        if (hasChanged) {
            checkScreensShared()
        }

        return hasChanged
    }

    /**
     * Method for updating participant screen sharing
     *
     * @param session [ChatSession] of a participant
     * @return True, if there have been changes. False, otherwise
     */
    fun changesInScreenSharing(session: ChatSession): Boolean {
        var hasChanged = false
        var participantSharingScreen: Participant? = null
        var participantSharingScreenForSpeaker: Participant? = null

        participants.value = participants.value?.map { participant ->
            return@map when {
                participant.peerId == session.peerId && participant.clientId == session.clientId && participant.isPresenting != session.hasScreenShare -> {
                    hasChanged = true
                    if (session.hasScreenShare) {
                        participantSharingScreen = participant
                    }

                    participant.copy(isPresenting = session.hasScreenShare)

                }

                else -> participant
            }
        }?.toMutableList()

        participantSharingScreen?.let {
            if (_state.value.callUIStatus == CallUIStatusType.SpeakerView) {
                pinSpeaker(it)
            }
        } ?: run {
            speakerParticipants.value = speakerParticipants.value?.map { speakerParticipant ->
                return@map when {
                    speakerParticipant.peerId == session.peerId && speakerParticipant.clientId == session.clientId && speakerParticipant.isPresenting != session.hasScreenShare -> {
                        if (!session.hasScreenShare) {
                            getAnotherParticipantWhoIsPresenting(speakerParticipant)?.let { newSpeaker ->
                                participantSharingScreenForSpeaker = newSpeaker
                            }
                        }

                        hasChanged = true
                        speakerParticipant.copy(isPresenting = session.hasScreenShare)
                    }

                    else -> speakerParticipant
                }
            }?.toMutableList()
        }

        participantSharingScreenForSpeaker?.let {
            if (_state.value.callUIStatus == CallUIStatusType.SpeakerView) {
                pinSpeaker(it)
            }
        }

        if (hasChanged) {
            checkScreensShared()
        }

        return hasChanged
    }

    /**
     * Get another participant who is presenting
     *
     * @param currentSpeaker    [Participant]
     * @return  [Participant]
     */
    private fun getAnotherParticipantWhoIsPresenting(currentSpeaker: Participant): Participant? {
        participants.value?.filter { it.isPresenting && (it.peerId != currentSpeaker.peerId || it.clientId != currentSpeaker.clientId) }
            ?.apply {
                return if (isNotEmpty()) first() else null
            }

        return null
    }

    /**
     * Method for updating participant audio
     *
     * @param session [ChatSession] of a participant
     * @return True, if there have been changes. False, otherwise
     */
    fun changesInRemoteAudioFlag(session: ChatSession): Boolean {
        var hasChanged = false
        participants.value = participants.value?.map { participant ->
            return@map when {
                participant.peerId == session.peerId && participant.clientId == session.clientId &&
                        (participant.isAudioOn != session.hasAudio || participant.isAudioDetected != session.isAudioDetected) -> {
                    hasChanged = true
                    participant.copy(isAudioOn = session.hasAudio)
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
        CallUtil.getCallsParticipating()?.apply {
            if (isEmpty()) {
                return
            }

            forEach { chatIdOfAnotherCall ->
                viewModelScope.launch {
                    runCatching {
                        getChatCallUseCase(chatIdOfAnotherCall)
                    }.onSuccess { anotherChatCall ->
                        anotherChatCall?.let { anotherCall ->
                            if (size == 1) {
                                if (chatIdOfCurrentCall != anotherCall.chatId && !anotherCall.isOnHold) {
                                    Timber.d("Another call on hold before join the meeting")
                                    putCallOnHoldOrResumeCall(
                                        chatId = anotherCall.chatId,
                                        setOnHold = true
                                    )
                                }
                            } else {
                                if (chatIdOfCurrentCall != anotherCall.chatId && !anotherCall.isOnHold) {
                                    Timber.d("Hang up one of the current calls in order to join the meeting")
                                    hangUpSpecificCall(anotherCall.callId)
                                }
                            }
                        }
                    }.onFailure { exception ->
                        Timber.e(exception)
                    }
                }
            }
        }
    }

    /**
     * Ignore call
     */
    private fun ignoreCall() {
        viewModelScope.launch {
            runCatching {
                setIgnoredCallUseCase(_state.value.currentChatId)
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
     * Method for remove incoming call notification
     */
    private fun removeIncomingCallNotification(chatId: Long) {
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
            viewModelScope.launch {
                runCatching {
                    setChatTitleUseCase(chatId = _state.value.currentChatId, title = newTitle)
                }.onSuccess { chatRequest ->
                    if (_state.value.currentChatId == chatRequest.chatHandle) {
                        _state.update { state ->
                            state.copy(
                                chatTitle = chatRequest.text ?: "",
                            )
                        }
                    }
                }.onFailure { exception ->
                    Timber.e(exception)
                }
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
     * @param session [ChatSession] of a participant
     * @param chatId Chat ID
     */
    fun requestHiResVideo(
        session: ChatSession?,
        chatId: Long,
    ) = session?.apply {
        if (!canReceiveVideoHiRes && isHiResVideo) {
            viewModelScope.launch {
                runCatching {
                    Timber.d("Request HiRes for remote video, clientId $clientId")
                    requestHighResolutionVideoUseCase(chatId, clientId)
                }.onFailure { exception ->
                    Timber.e(exception)
                }.onSuccess { request ->
                    Timber.d("Request high res video: chatId = ${request.chatHandle}, hires? ${request.flag}, clientId = ${request.userHandle}")
                }
            }
        }
    }

    /**
     * Remove High Resolution for remote video
     *
     * @param session [ChatSession] of a participant
     * @param chatId Chat ID
     */
    fun stopHiResVideo(
        session: ChatSession?,
        chatId: Long,
    ) = session?.apply {
        if (canReceiveVideoHiRes) {
            viewModelScope.launch {
                runCatching {
                    Timber.d("Stop HiRes for remote video, clientId $clientId")
                    stopHighResolutionVideoUseCase(chatId, clientId)
                }.onFailure { exception ->
                    Timber.e(exception)
                }.onSuccess { request ->
                    Timber.d("Stop high res video: chatId = ${request.chatHandle}, hires? ${request.flag}, clientId = ${request.userHandle}")
                }
            }
        }
    }

    /**
     * Add Low Resolution for remote video
     *
     * @param session [ChatSession] of a participant
     * @param chatId Chat ID
     */
    fun requestLowResVideo(
        session: ChatSession?,
        chatId: Long,
    ) = session?.apply {
        if (!canReceiveVideoLowRes && isLowResVideo) {
            viewModelScope.launch {
                runCatching {
                    Timber.d("Request LowRes for remote video, clientId $clientId")
                    requestLowResolutionVideoUseCase(chatId, clientId)
                }.onFailure { exception ->
                    Timber.e(exception)
                }.onSuccess { request ->
                    Timber.d("Request low res video: chatId = ${request.chatHandle}, lowRes? ${request.flag}, clientId = ${request.userHandle}")
                }
            }
        }
    }

    /**
     * Remove Low Resolution for remote video
     *
     * @param session [ChatSession] of a participant
     * @param chatId Chat ID
     */
    private fun stopLowResVideo(
        session: ChatSession?,
        chatId: Long,
    ) = session?.apply {
        if (canReceiveVideoLowRes) {
            viewModelScope.launch {
                runCatching {
                    Timber.d("Stop LowRes for remote video, clientId $clientId")
                    stopLowResolutionVideoUseCase(chatId, clientId)
                }.onFailure { exception ->
                    Timber.e(exception)
                }.onSuccess { request ->
                    Timber.d("Stop low res video: chatId = ${request.chatHandle}, lowRes? ${request.flag}, clientId = ${request.userHandle}")
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
                getSessionByClientId(participant.clientId)?.let { _ ->
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

        composite.clear()

        LiveEventBus.get<Pair<Long, Boolean>>(EventConstants.EVENT_UPDATE_WAITING_FOR_OTHERS)
            .removeObserver(waitingForOthersBannerObserver)
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
    private fun numParticipants(): Int {
        participants.value?.size?.let { numParticipants ->
            return numParticipants
        }

        return 0
    }

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
    private fun amIAModerator(): Boolean = getOwnPrivileges() == PRIV_MODERATOR

    /**
     * Determine if the participant has standard privileges
     *
     * @param peerId User handle of a participant
     */
    fun isStandardUser(peerId: Long): Boolean =
        inMeetingRepository.getChatRoom(_state.value.currentChatId)
            ?.let { it.getPeerPrivilegeByHandle(peerId) == MegaChatRoom.PRIV_STANDARD }
            ?: run { false }

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

    private inline val Context.defaultSharedPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(this)

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
    private fun updateMeetingInfoBottomPanel() {
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
        speakerParticipants.value?.filter { it.isSpeaker }?.apply {
            return if (isNotEmpty()) first() else null
        }

        return null
    }

    /**
     * Method to remove the current speaker
     */
    private fun removeCurrentSpeaker() {
        speakerParticipants.value?.filter { it.isSpeaker }?.forEach { participant ->
            participant.videoListener?.let {
                removeResolutionAndListener(participant, it)
                participant.videoListener = null
            }
            speakerParticipants.value?.indexOf(participant)?.let { position ->
                if (position != INVALID_POSITION) {
                    speakerParticipants.value?.removeAt(position)
                    Timber.d("Num of speaker participants: ${speakerParticipants.value?.size}")
                    speakerParticipants.value = speakerParticipants.value
                }
            }
        }
    }

    /**
     * Method to eliminate which are no longer speakers
     */
    fun removePreviousSpeakers() {
        speakerParticipants.value?.filter { !it.isSpeaker }?.forEach { participant ->
            participant.videoListener?.let {
                removeResolutionAndListener(participant, it)
                participant.videoListener = null
            }
            speakerParticipants.value?.indexOf(participant)?.let { position ->
                if (position != INVALID_POSITION) {
                    speakerParticipants.value?.removeAt(position)
                    Timber.d("Num of speaker participants: ${speakerParticipants.value?.size}")
                    speakerParticipants.value = speakerParticipants.value
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
     * Method that controls whether a participant's options (3 dots) should be enabled or not
     *
     * @param participantIsMe If the participant is me
     * @param participantIsGuest If the participant is a guest
     * @return True, if should be enabled. False, if not
     */
    private fun shouldParticipantsOptionBeVisible(
        participantIsMe: Boolean,
        participantIsGuest: Boolean,
    ): Boolean = !((!amIAModerator() && participantIsGuest) ||
            (amIAGuest() && participantIsMe) ||
            (!amIAModerator() && amIAGuest() && !participantIsMe))

    /**
     * End for all specified call
     *
     * @param chatId Chat ID
     */
    private fun endCallForAll(chatId: Long) = viewModelScope.launch {
        Timber.d("End for all. Chat id $chatId")
        runCatching {
            endCallUseCase(chatId)
        }.onSuccess {
            broadcastCallEndedUseCase(chatId)
        }.onFailure {
            Timber.e(it.stackTraceToString())
        }
    }

    /**
     * End for all the current call
     */
    fun endCallForAll() {
        state.value.call?.let { call ->
            endCallForAll(call.chatId)
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
    private fun hangCall(callId: Long) = viewModelScope.launch {
        Timber.d("Hang up call. Call id $callId")

        runCatching {
            hangChatCallUseCase(callId)
        }.onSuccess {
            broadcastCallEndedUseCase(state.value.currentChatId)

        }.onFailure {
            Timber.e(it.stackTraceToString())
        }
    }

    /**
     * Hang up the current call
     */
    fun hangCurrentCall() {
        state.value.call?.apply {
            hangCall(callId)
        }
    }

    /**
     * Control when the hang up button is clicked
     */
    fun checkClickEndButton() {
        if (isOneToOneCall() || amIAGuest() || numParticipants() == 0) {
            hangCurrentCall()
            return
        }

        state.value.chat?.let { chat ->
            when (chat.ownPrivilege) {
                ChatRoomPermission.Moderator -> _state.update { state ->
                    val shouldAssignHost = chat.isMeeting && shouldAssignModerator()
                    state.copy(
                        showEndMeetingAsHostBottomPanel = !shouldAssignHost,
                        showEndMeetingAsOnlyHostBottomPanel = shouldAssignHost,
                    )
                }

                else -> hangCurrentCall()
            }
        }
    }

    /**
     * Hide bottom panels
     */
    fun hideBottomPanels() {
        _state.update { state ->
            state.copy(
                showEndMeetingAsHostBottomPanel = false,
                showEndMeetingAsOnlyHostBottomPanel = false,
            )
        }
    }

    /**
     * Set call UI status
     *
     * @param newStatus [CallUIStatusType]
     */
    fun setStatus(newStatus: CallUIStatusType) {
        if (_state.value.callUIStatus != newStatus && newStatus == CallUIStatusType.SpeakerView) {
            sortParticipantsListForSpeakerView()
        }

        _state.update { it.copy(callUIStatus = newStatus) }
    }

    /**
     * On update list consumed
     */
    fun onUpdateListConsumed() =
        _state.update { it.copy(updateListUi = false) }

    /**
     * Sort participants list for speaker view
     *
     * @param participantAtTheBeginning [Participant] to be placed first in the carousel
     */
    private fun sortParticipantsListForSpeakerView(participantAtTheBeginning: Participant? = null) {
        Timber.d("Sort participant list")
        participantAtTheBeginning?.apply {
            participants.value?.sortByDescending { it.peerId == peerId }
        }
        participants.value?.sortByDescending { it.isPresenting }
        _state.update { it.copy(updateListUi = true) }
    }

    /**
     * On reject button tapped
     *
     * @param chatId Chat id of the incoming call
     */
    fun onRejectBottomTap(chatId: Long) {
        removeIncomingCallNotification(chatId)
        when {
            isOneToOneCall() -> checkClickEndButton()
            else -> ignoreCall()
        }
    }

    /**
     * Dismiss more call options panel
     */
    fun moreCallOptionsBottomPanelDismiss() =
        _state.update { it.copy(showCallOptionsBottomSheet = false) }

    /**
     * More call options clicked
     */
    fun onClickMoreCallOptions() {
        _state.update { it.copy(showCallOptionsBottomSheet = true) }
    }

    /**
     * Raised hand to speak
     */
    fun raiseHandToSpeak() {
        viewModelScope.launch {
            runCatching {
                raiseHandToSpeakUseCase(_state.value.currentChatId)
            }.onSuccess {
                moreCallOptionsBottomPanelDismiss()
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Lower hand to stop speak
     */
    fun lowerHandToStopSpeak() {
        viewModelScope.launch {
            runCatching {
                lowerHandToStopSpeakUseCase(_state.value.currentChatId)
            }.onSuccess {
                moreCallOptionsBottomPanelDismiss()
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Put call on hold clicked
     */
    fun onClickOnHold() {
        when (state.value.getButtonTypeToShow) {
            CallOnHoldType.ResumeCall -> putCallOnHoldOrResumeCall(
                chatId = state.value.currentChatId,
                setOnHold = false
            )

            CallOnHoldType.PutCallOnHold -> putCallOnHoldOrResumeCall(
                chatId = state.value.currentChatId,
                setOnHold = true
            )

            CallOnHoldType.SwapCalls -> swapCalls()
        }
    }

    /**
     * Check swap calls
     */
    private fun swapCalls() {
        putCallOnHoldOrResumeCall(
            chatId = state.value.currentChatId,
            setOnHold = state.value.isCallOnHold == false
        )

        state.value.anotherCall?.let {
            putCallOnHoldOrResumeCall(
                chatId = it.chatId,
                setOnHold = !it.isOnHold
            )
        }
    }

    /**
     * Put call on hold or resume call
     *
     * @param chatId    Chat id.
     * @param setOnHold True, put call on hold. False, resume call.
     */
    private fun putCallOnHoldOrResumeCall(chatId: Long, setOnHold: Boolean) {
        viewModelScope.launch {
            runCatching {
                holdChatCallUseCase(chatId = chatId, setOnHold = setOnHold)
            }.onSuccess {
                moreCallOptionsBottomPanelDismiss()
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Is ephemeral account plus plus
     */
    private fun isEphemeralAccount() {
        if (state.value.isEphemeralAccount != null) return

        viewModelScope.launch {
            runCatching {
                val isEphemeralAccount = isEphemeralPlusPlusUseCase()
                _state.update { state -> state.copy(isEphemeralAccount = isEphemeralAccount) }
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Trigger event to show snackbarInSpeakerViewMessage
     *
     * @param message     Content for snack bar
     */
    private fun triggerSnackbarInSpeakerViewMessage(message: String) {
        onSnackbarInSpeakerViewMessageConsumed()
        _state.update { it.copy(snackbarInSpeakerViewMessage = triggered(message)) }
    }

    /**
     * Reset and notify that snackbarInSpeakerViewMessage is consumed
     */
    fun onSnackbarInSpeakerViewMessageConsumed() {
        _state.update {
            it.copy(snackbarInSpeakerViewMessage = consumed())
        }
    }

    /**
     * Trigger event to show OnlyMeEndCallTimer
     */
    fun showOnlyMeEndCallTimer(time: Long) {
        _state.update {
            it.copy(showOnlyMeEndCallTime = time)
        }
    }

    /**
     * Hide OnlyMeEndCallTimer
     */
    fun hideOnlyMeEndCallTimer() {
        _state.update {
            it.copy(showOnlyMeEndCallTime = null)
        }
    }

    /**
     * Trigger event to show participants changes message
     */
    fun showParticipantChangesMessage(message: String, type: Int) {
        when (type) {
            TYPE_JOIN -> {
                _state.update {
                    it.copy(
                        participantsChanges = ParticipantsChange(
                            message,
                            ParticipantsChangeType.Join
                        )
                    )
                }
            }

            TYPE_LEFT -> {
                _state.update {
                    it.copy(
                        participantsChanges = ParticipantsChange(
                            message,
                            ParticipantsChangeType.Left
                        )
                    )
                }
            }
        }
    }

    /**
     * Hide participant changes message
     */
    fun hideParticipantChangesMessage() {
        _state.update {
            it.copy(participantsChanges = null)
        }
    }

    /**
     * Set raised hand suggestion shown
     */
    fun setRaisedHandSuggestionShown() {
        if (_state.value.isRaiseToSpeakFeatureFlagEnabled.not() || isOneToOneCall()) return
        viewModelScope.launch {
            runCatching {
                setRaiseToHandSuggestionShownUseCase()
                hideRaiseToHandPopup()
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Hide raise to hand popup
     */
    fun hideRaiseToHandPopup() {
        if (_state.value.isRaiseToSpeakFeatureFlagEnabled.not() || isOneToOneCall()) return
        _state.update {
            it.copy(
                isRaiseToHandSuggestionShown = true
            )
        }
    }

    /**
     * Check if raise to hand feature tooltip is shown
     */
    fun checkRaiseToHandFeatureTooltipIsShown() {
        if (_state.value.isRaiseToSpeakFeatureFlagEnabled.not() || isOneToOneCall()) return
        viewModelScope.launch {
            runCatching {
                val value = isRaiseToHandSuggestionShownUseCase()
                _state.update { it.copy(isRaiseToHandSuggestionShown = value) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Update PiP mode value
     */
    fun updateIsInPipMode(isInPipMode: Boolean) {
        viewModelScope.launch {
            if (isInPipMode) {
                setStatus(newStatus = CallUIStatusType.PictureInPictureView)
            }
            _state.update {
                it.copy(isInPipMode = isInPipMode)
            }
        }
    }
}
