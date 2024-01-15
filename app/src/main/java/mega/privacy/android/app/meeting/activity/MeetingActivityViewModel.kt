package mega.privacy.android.app.meeting.activity

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.jeremyliao.liveeventbus.LiveEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.constants.EventConstants
import mega.privacy.android.app.constants.EventConstants.EVENT_AUDIO_OUTPUT_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_CHAT_TITLE_CHANGE
import mega.privacy.android.app.constants.EventConstants.EVENT_MEETING_CREATED
import mega.privacy.android.app.extensions.updateItemAt

import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.listeners.InviteToChatRoomListener
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.main.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.meeting.listeners.DisableAudioVideoCallListener
import mega.privacy.android.app.meeting.listeners.IndividualCallVideoListener
import mega.privacy.android.app.meeting.listeners.OpenVideoDeviceListener
import mega.privacy.android.app.presentation.chat.model.AnswerCallResult
import mega.privacy.android.app.presentation.contactinfo.model.ContactInfoState
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.meeting.mapper.ChatParticipantMapper
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.app.usecase.call.GetCallUseCase
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.amIParticipatingInAChat
import mega.privacy.android.app.utils.ChatUtil.getTitleChat
import mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CREATING_JOINING_MEETING
import mega.privacy.android.app.utils.Constants.REQUEST_ADD_PARTICIPANTS
import mega.privacy.android.app.utils.VideoCaptureUtils
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.meeting.CallType
import mega.privacy.android.domain.entity.meeting.ChatCallChanges
import mega.privacy.android.domain.entity.meeting.ChatCallStatus
import mega.privacy.android.domain.entity.meeting.ChatSessionChanges
import mega.privacy.android.domain.entity.meeting.MeetingParticipantNotInCallStatus
import mega.privacy.android.domain.entity.meeting.ParticipantsSection
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.CheckChatLinkUseCase
import mega.privacy.android.domain.usecase.CreateChatLink
import mega.privacy.android.domain.usecase.GetChatParticipants
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.QueryChatLink
import mega.privacy.android.domain.usecase.RemoveFromChat
import mega.privacy.android.domain.usecase.SetOpenInvite
import mega.privacy.android.domain.usecase.UpdateChatPermissions
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.IsEphemeralPlusPlusUseCase
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import mega.privacy.android.domain.usecase.login.MonitorFinishActivityUseCase
import mega.privacy.android.domain.usecase.meeting.AllowUsersJoinCallUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastCallEndedUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastCallRecordingConsentEventUseCase
import mega.privacy.android.domain.usecase.meeting.GetChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.meeting.HangChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorCallEndedUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.RingIndividualInACallUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaChatRequest
import nz.mega.sdk.MegaChatRequestListenerInterface
import nz.mega.sdk.MegaChatRoom
import nz.mega.sdk.MegaError
import nz.mega.sdk.MegaRequest
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * It's very common that two or more fragments in Meeting activity need to communicate with each other.
 * These fragments can share a ViewModel using their activity scope to handle this communication.
 * MeetingActivityViewModel shares state of Mic, Camera and Speaker for all Fragments
 *
 * @property meetingActivityRepository                      [MeetingActivityRepository]
 * @property answerChatCallUseCase                          [AnswerChatCallUseCase]
 * @property getCallUseCase                                 [GetCallUseCase]
 * @property rtcAudioManagerGateway                         [RTCAudioManagerGateway]
 * @property getChatParticipants                            [GetChatParticipants]
 * @property chatManagement                                 [ChatManagement]
 * @property setChatVideoInDeviceUseCase                    [SetChatVideoInDeviceUseCase]
 * @property checkChatLink                                  [CheckChatLinkUseCase]
 * @property logoutUseCase                                  [LogoutUseCase]
 * @property monitorFinishActivityUseCase                   [MonitorFinishActivityUseCase]
 * @property monitorChatCallUpdatesUseCase                  [MonitorChatCallUpdatesUseCase]
 * @property getChatRoomUseCase                             [GetChatRoomUseCase]
 * @property getChatCallUseCase                             [GetChatCallUseCase]
 * @property getFeatureFlagValue                            [GetFeatureFlagValueUseCase]
 * @property setOpenInvite                                  [SetOpenInvite]
 * @property chatParticipantMapper                          [ChatParticipantMapper]
 * @property monitorChatRoomUpdates                         [MonitorChatRoomUpdates]
 * @property queryChatLink                                  [QueryChatLink]
 * @property isEphemeralPlusPlusUseCase                     [IsEphemeralPlusPlusUseCase]
 * @property createChatLink                                 [CreateChatLink]
 * @property inviteContactUseCase                           [InviteContactUseCase]
 * @property updateChatPermissionsUseCase                   [UpdateChatPermissions]
 * @property removeFromChaUseCase                           [RemoveFromChat]
 * @property startConversationUseCase                       [StartConversationUseCase]
 * @property isConnectedToInternetUseCase                   [IsConnectedToInternetUseCase]
 * @property monitorStorageStateEventUseCase                [MonitorStorageStateEventUseCase]
 * @property hangChatCallUseCase                            [HangChatCallUseCase]
 * @property broadcastCallRecordingConsentEventUseCase      [BroadcastCallRecordingConsentEventUseCase]
 * @property monitorCallEndedUseCase                        [MonitorCallEndedUseCase]
 * @property broadcastCallEndedUseCase                      [BroadcastCallEndedUseCase]
 * @property monitorScheduledMeetingUpdatesUseCase          [MonitorScheduledMeetingUpdatesUseCase]
 * @property getMyFullNameUseCase                           [GetMyFullNameUseCase]
 * @property deviceGateway                                  [DeviceGateway]
 * @property monitorUserUpdates                             [MonitorUserUpdates]
 * @property ringIndividualInACallUseCase                   [RingIndividualInACallUseCase]
 * @property allowUsersJoinCallUseCase                      [AllowUsersJoinCallUseCase]
 * @property state                                          Current view state as [MeetingState]
 */
@HiltViewModel
class MeetingActivityViewModel @Inject constructor(
    private val meetingActivityRepository: MeetingActivityRepository,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val getCallUseCase: GetCallUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val chatManagement: ChatManagement,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val checkChatLink: CheckChatLinkUseCase,
    private val getChatParticipants: GetChatParticipants,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val monitorFinishActivityUseCase: MonitorFinishActivityUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val queryChatLink: QueryChatLink,
    private val getFeatureFlagValue: GetFeatureFlagValueUseCase,
    private val setOpenInvite: SetOpenInvite,
    private val chatParticipantMapper: ChatParticipantMapper,
    private val isEphemeralPlusPlusUseCase: IsEphemeralPlusPlusUseCase,
    private val createChatLink: CreateChatLink,
    private val inviteContactUseCase: InviteContactUseCase,
    private val updateChatPermissionsUseCase: UpdateChatPermissions,
    private val removeFromChaUseCase: RemoveFromChat,
    private val startConversationUseCase: StartConversationUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val broadcastCallRecordingConsentEventUseCase: BroadcastCallRecordingConsentEventUseCase,
    private val monitorCallEndedUseCase: MonitorCallEndedUseCase,
    private val broadcastCallEndedUseCase: BroadcastCallEndedUseCase,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val getMyFullNameUseCase: GetMyFullNameUseCase,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val monitorScheduledMeetingUpdatesUseCase: MonitorScheduledMeetingUpdatesUseCase,
    private val deviceGateway: DeviceGateway,
    private val ringIndividualInACallUseCase: RingIndividualInACallUseCase,
    private val allowUsersJoinCallUseCase: AllowUsersJoinCallUseCase,
    @ApplicationContext private val context: Context,
) : BaseRxViewModel(), OpenVideoDeviceListener.OnOpenVideoDeviceCallback,
    DisableAudioVideoCallListener.OnDisableAudioVideoCallback {

    private val _state = MutableStateFlow(MeetingState())
    val state: StateFlow<MeetingState> = _state

    /**
     * Check if is online (connected to Internet)
     *
     * @return True if it is only or False otherwise.
     */
    fun isOnline(): Boolean = isConnectedToInternetUseCase()

    /**
     * Check if it's 24 hour format
     */
    val is24HourFormat by lazy { deviceGateway.is24HourFormat() }

    /**
     * Get latest [StorageState] from [MonitorStorageStateEventUseCase] use case.
     * @return the latest [StorageState]
     */
    fun getStorageState(): StorageState = monitorStorageStateEventUseCase.getState()

    // Avatar
    private val _avatarLiveData = MutableLiveData<Bitmap>()
    val avatarLiveData: LiveData<Bitmap> = _avatarLiveData

    var tips: MutableLiveData<String> = MutableLiveData<String>()

    // OnOffFab
    private val _micLiveData: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    private val _cameraLiveData: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    private val _speakerLiveData: MutableLiveData<AppRTCAudioManager.AudioDevice> =
        MutableLiveData<AppRTCAudioManager.AudioDevice>().apply {
            val audioManager = rtcAudioManagerGateway.audioManager
            value = audioManager?.selectedAudioDevice ?: AppRTCAudioManager.AudioDevice.NONE
        }

    val micLiveData: LiveData<Boolean> = _micLiveData
    val cameraLiveData: LiveData<Boolean> = _cameraLiveData
    val speakerLiveData: LiveData<AppRTCAudioManager.AudioDevice> = _speakerLiveData

    // Permissions
    private val _cameraGranted = MutableStateFlow(false)
    val cameraGranted: StateFlow<Boolean> get() = _cameraGranted
    private val _recordAudioGranted = MutableStateFlow(false)
    val recordAudioGranted: StateFlow<Boolean> get() = _recordAudioGranted

    private val _cameraPermissionCheck = MutableLiveData<Boolean>()
    val cameraPermissionCheck: LiveData<Boolean> = _cameraPermissionCheck

    private val _recordAudioPermissionCheck = MutableLiveData<Boolean>()
    val recordAudioPermissionCheck: LiveData<Boolean> = _recordAudioPermissionCheck

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent =
        monitorConnectivityUseCase().shareIn(viewModelScope, SharingStarted.Eagerly)

    // Name of meeting
    private val _meetingNameLiveData: MutableLiveData<String> = MutableLiveData<String>()
    val meetingNameLiveData: LiveData<String> = _meetingNameLiveData

    // Show snack bar
    private val _snackBarLiveData = MutableLiveData("")
    val snackBarLiveData: LiveData<String> = _snackBarLiveData

    //Control when call should be switched
    private val _switchCall = MutableStateFlow(MEGACHAT_INVALID_HANDLE)
    val switchCall: StateFlow<Long> get() = _switchCall

    //Control when call should be finish
    private val _finishMeetingActivity = MutableStateFlow(false)
    val finishMeetingActivity: StateFlow<Boolean> get() = _finishMeetingActivity

    private val audioOutputStateObserver =
        Observer<AppRTCAudioManager.AudioDevice> {
            if (_speakerLiveData.value != it && it != AppRTCAudioManager.AudioDevice.NONE) {
                Timber.d("Updating speaker $it")

                _speakerLiveData.value = it
                tips.value = when (it) {
                    AppRTCAudioManager.AudioDevice.EARPIECE -> context.getString(R.string.general_speaker_off)
                    AppRTCAudioManager.AudioDevice.SPEAKER_PHONE -> context.getString(R.string.general_speaker_on)
                    else -> context.getString(R.string.general_headphone_on)
                }
            }
        }

    private val meetingCreatedObserver =
        Observer<Long> {
            updateChatRoomId(it)
            MegaApplication.isWaitingForCall = true
        }

    private val titleMeetingChangeObserver =
        Observer<MegaChatRoom> { chatRoom ->
            meetingActivityRepository.getChatRoom(_state.value.chatId)?.let {
                if (it.chatId == chatRoom.chatId) {
                    _meetingNameLiveData.value = getTitleChat(it)
                }
            }
        }

    /**
     * Send event that in Meeting fragment is visible or not
     *
     * @param isVisible True if the fragment it visible, false otherwise.
     */
    fun sendEnterCallEvent(isVisible: Boolean) = LiveEventBus.get(
        EventConstants.EVENT_ENTER_IN_MEETING,
        Boolean::class.java
    ).post(isVisible)

    init {
        LiveEventBus.get(EVENT_AUDIO_OUTPUT_CHANGE, AppRTCAudioManager.AudioDevice::class.java)
            .observeForever(audioOutputStateObserver)

        LiveEventBus.get(EVENT_CHAT_TITLE_CHANGE, MegaChatRoom::class.java)
            .observeForever(titleMeetingChangeObserver)

        LiveEventBus.get(EVENT_MEETING_CREATED, Long::class.java)
            .observeForever(meetingCreatedObserver)

        startMonitoringChatCallUpdates()
        startMonitorChatSessionUpdates()
        getMyFullName()

        getCallUseCase.getCallEnded()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { chatIdOfCallEnded ->
                    if (chatIdOfCallEnded == _state.value.chatId) {
                        resetCallRecordingState()
                        viewModelScope.launch { broadcastCallEndedUseCase(chatIdOfCallEnded) }
                        _finishMeetingActivity.value = true
                    }
                },
                onError = Timber::e
            )
            .addTo(composite)

        viewModelScope.launch {
            monitorCallEndedUseCase().conflate().collect { chatId ->
                if (chatId == state.value.chatId) {
                    resetCallRecordingState()
                }
            }
        }

        // Show the default avatar (the Alphabet avatar) above all, then load the actual avatar
        showDefaultAvatar().invokeOnCompletion {
            loadAvatar(true)
        }

        viewModelScope.launch {
            flow {
                emitAll(monitorUserUpdates()
                    .catch { Timber.w("Exception monitoring user updates: $it") }
                    .filter { it == UserChanges.Firstname || it == UserChanges.Lastname || it == UserChanges.Email })
            }.collect {
                when (it) {
                    UserChanges.Firstname, UserChanges.Lastname -> getMyFullName()
                    else -> Unit
                }
            }
        }
    }

    /**
     * Control when calls are to be switched
     */
    fun clickSwitchCall() {
        checkAnotherCalls(false)
    }

    /**
     * Control when call should be finish
     */
    fun clickEndCall() {
        checkAnotherCalls(true)
    }

    /**
     * Determine if I am a guest
     *
     * @return True, if I am a guest. False if not
     */
    fun amIAGuest(): Boolean = meetingActivityRepository.amIAGuest()

    /**
     * Get my full name
     */
    private fun getMyFullName() = viewModelScope.launch {
        runCatching {
            getMyFullNameUseCase()
        }.onSuccess {
            it?.apply {
                _state.update { state ->
                    state.copy(
                        myFullName = this,
                    )
                }
            }
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Get chat room
     */
    private fun getChatRoom() = viewModelScope.launch {
        runCatching {
            getChatRoomUseCase(_state.value.chatId)
        }.onSuccess { chatRoom ->
            chatRoom?.apply {
                _state.update {
                    it.copy(
                        myPermission = ownPrivilege,
                        isOpenInvite = isOpenInvite || ownPrivilege == ChatRoomPermission.Moderator,
                        enabledAllowNonHostAddParticipantsOption = isOpenInvite,
                        hasWaitingRoom = isWaitingRoom,
                        title = title,
                        callType = when {
                            isMeeting -> CallType.Meeting
                            !isMeeting && isGroup -> CallType.Group
                            else -> CallType.OneToOne
                        }
                    )
                }

                queryMeetingLink(shouldShareMeetingLink = false)
            }

        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Get chat call
     */
    fun getChatCall(checkEphemeralAccount: Boolean = true) {
        _state.update {
            it.copy(
                isNecessaryToUpdateCall = false
            )
        }
        viewModelScope.launch {
            runCatching {
                getChatCallUseCase(_state.value.chatId)
            }.onSuccess { call ->
                call?.let {
                    checkIfPresenting(it)
                    if (checkEphemeralAccount) {
                        checkEphemeralAccountAndWaitingRoom(it)
                    }
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    private fun getScheduledMeeting() =
        viewModelScope.launch {
            runCatching {
                getScheduledMeetingByChat(state.value.chatId)
            }.onFailure {
                Timber.d("Scheduled meeting does not exist")
                _state.update {
                    it.copy(
                        chatScheduledMeeting = null
                    )
                }
            }.onSuccess { scheduledMeetingList ->
                scheduledMeetingList?.let { list ->
                    list.forEach { scheduledMeetReceived ->
                        if (scheduledMeetReceived.parentSchedId == -1L) {
                            _state.update {
                                it.copy(
                                    chatScheduledMeeting = scheduledMeetReceived
                                )
                            }
                            return@forEach
                        }
                    }
                }
            }
        }


    /**
     * Get scheduled meeting updates
     */
    private fun startMonitorScheduledMeetingUpdates() =
        viewModelScope.launch {
            monitorScheduledMeetingUpdatesUseCase().collectLatest { scheduledMeetReceived ->
                if (scheduledMeetReceived.chatId != state.value.chatId) {
                    return@collectLatest
                }

                if (scheduledMeetReceived.parentSchedId != -1L) {
                    return@collectLatest
                }

                scheduledMeetReceived.changes?.let { changes ->
                    changes.forEach {
                        Timber.d("Monitor scheduled meeting updated, changes $changes")
                        if (_state.value.chatScheduledMeeting == null) {
                            _state.update { state ->
                                state.copy(
                                    chatScheduledMeeting = scheduledMeetReceived
                                )
                            }
                            return@forEach
                        }

                        when (it) {
                            ScheduledMeetingChanges.NewScheduledMeeting ->
                                _state.update { state ->
                                    state.copy(
                                        chatScheduledMeeting = scheduledMeetReceived
                                    )
                                }

                            ScheduledMeetingChanges.Title ->
                                _state.update { state ->
                                    state.copy(
                                        chatScheduledMeeting = state.chatScheduledMeeting?.copy(
                                            title = scheduledMeetReceived.title
                                        )
                                    )
                                }

                            ScheduledMeetingChanges.Description ->
                                _state.update { state ->
                                    state.copy(
                                        chatScheduledMeeting = state.chatScheduledMeeting?.copy(
                                            title = scheduledMeetReceived.description
                                        )
                                    )
                                }

                            ScheduledMeetingChanges.StartDate -> _state.update { state ->
                                state.copy(
                                    chatScheduledMeeting = state.chatScheduledMeeting?.copy(
                                        startDateTime = scheduledMeetReceived.startDateTime,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.EndDate,
                            -> _state.update { state ->
                                state.copy(
                                    chatScheduledMeeting = state.chatScheduledMeeting?.copy(
                                        startDateTime = scheduledMeetReceived.endDateTime,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.ParentScheduledMeetingId -> _state.update { state ->
                                state.copy(
                                    chatScheduledMeeting = state.chatScheduledMeeting?.copy(
                                        parentSchedId = scheduledMeetReceived.parentSchedId,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.TimeZone -> _state.update { state ->
                                state.copy(
                                    chatScheduledMeeting = state.chatScheduledMeeting?.copy(
                                        timezone = scheduledMeetReceived.timezone,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.Attributes -> _state.update { state ->
                                state.copy(
                                    chatScheduledMeeting = state.chatScheduledMeeting?.copy(
                                        attributes = scheduledMeetReceived.attributes,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.OverrideDateTime -> _state.update { state ->
                                state.copy(
                                    chatScheduledMeeting = state.chatScheduledMeeting?.copy(
                                        overrides = scheduledMeetReceived.overrides,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.ScheduledMeetingsFlags -> _state.update { state ->
                                state.copy(
                                    chatScheduledMeeting = state.chatScheduledMeeting?.copy(
                                        flags = scheduledMeetReceived.flags,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.RepetitionRules -> _state.update { state ->
                                state.copy(
                                    chatScheduledMeeting = state.chatScheduledMeeting?.copy(
                                        rules = scheduledMeetReceived.rules,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.CancelledFlag -> _state.update { state ->
                                state.copy(
                                    chatScheduledMeeting = state.chatScheduledMeeting?.copy(
                                        isCanceled = scheduledMeetReceived.isCanceled,
                                    )
                                )
                            }

                            else -> {}
                        }
                    }
                }
            }
        }

    /**
     * Check if some participant is presenting
     *
     * @param call  [ChatCall]
     */
    private fun checkIfPresenting(call: ChatCall) {
        var isParticipantSharingScreen = false
        call.sessionByClientId.forEach { (_, value) ->
            if (value.hasScreenShare) {
                isParticipantSharingScreen = true
            }
        }

        _state.update { state ->
            state.copy(
                isParticipantSharingScreen = isParticipantSharingScreen
            )
        }
    }


    /**
     * Check ephemeral account and waiting room
     *
     * @param call  [ChatCall]
     */
    private fun checkEphemeralAccountAndWaitingRoom(call: ChatCall) {
        call.apply {
            checkEphemeralAccount()
            waitingRoom?.let { waitingRoom ->
                _state.update {
                    it.copy(
                        usersInWaitingRoomIDs = waitingRoom.peers ?: emptyList()
                    )
                }
                if (state.value.usersInWaitingRoomIDs.isNotEmpty()) {
                    updateParticipantsSection(ParticipantsSection.WaitingRoomSection)
                }
                checkParticipantLists()
            }
        }
    }

    /**
     * Check ephemeral account
     */
    private fun checkEphemeralAccount() = viewModelScope.launch {
        runCatching {
            isEphemeralPlusPlusUseCase()
        }.onSuccess { isEphemeralAccount ->
            _state.update {
                it.copy(
                    isGuest = isEphemeralAccount,
                )
            }
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Get chat call updates
     */
    private fun startMonitoringChatCallUpdates() =
        viewModelScope.launch {
            monitorChatCallUpdatesUseCase()
                .filter { it.chatId == _state.value.chatId }
                .collectLatest { call ->
                    checkIfPresenting(call)

                    call.changes?.apply {
                        if (contains(ChatCallChanges.Status)) {
                            if (call.status == ChatCallStatus.InProgress) {
                                checkEphemeralAccountAndWaitingRoom(call)
                            }

                        }
                        if (contains(ChatCallChanges.LocalAVFlags)) {
                            val isEnable = call.hasLocalAudio
                            _micLiveData.value = isEnable
                            Timber.d("open Mic: $isEnable")
                            tips.value = when (isEnable) {
                                true -> context.getString(
                                    R.string.general_mic_unmute
                                )

                                false -> context.getString(
                                    R.string.general_mic_mute
                                )
                            }
                        }

                        if (contains(ChatCallChanges.WaitingRoomUsersEntered) || contains(
                                ChatCallChanges.WaitingRoomUsersLeave
                            )
                        ) {
                            call.waitingRoom?.apply {
                                peers?.let { ids ->
                                    _state.update {
                                        it.copy(
                                            usersInWaitingRoomIDs = ids
                                        )
                                    }
                                    checkParticipantLists()
                                }
                            }
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
                .filter { it.chatId == _state.value.chatId }
                .collectLatest { result ->
                    result.session?.let { session ->
                        session.changes?.apply {
                            if (contains(ChatSessionChanges.RemoteAvFlags)) {
                                if (session.hasScreenShare) {
                                    _state.update { it.copy(isParticipantSharingScreen = true) }
                                } else {
                                    _state.update { it.copy(isNecessaryToUpdateCall = true) }
                                }
                            }
                            if (contains(ChatSessionChanges.SessionOnRecording)) {
                                _state.update { state ->
                                    state.copy(
                                        isSessionOnRecording = session.isRecording,
                                        showRecordingConsentDialog = if (!state.isRecordingConsentAccepted) session.isRecording else false,
                                        startOrStopRecordingParticipantName = if (state.callType == CallType.OneToOne) {
                                            state.title
                                        } else {
                                            state.usersInCall.find { participant -> participant.peerId == session.peerId }?.name.orEmpty()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
        }

    /**
     * Logout
     *
     * logs out the user from mega application and navigates to login activity
     * logic is handled at [MegaChatRequestHandler] onRequestFinished callback
     * we are setting isLoggingRunning true hence it will not be navigated to login page
     */
    fun logout() = viewModelScope.launch {
        runCatching {
            logoutUseCase()
        }.onSuccess {
            //We need to observe finish activity only when logout is success
            //We are waiting for the logout process in MegaChatRequestHandler to finish with this monitor flow and then triggers navigating to LeftMeetingActivity
            //if we navigate earlier MegaChatRequestHandler.isLoggingRunning will be false and app will navigate to default LoginActivity
            monitorFinishActivityUseCase().collect { logoutFinished ->
                _state.update { it.copy(shouldLaunchLeftMeetingActivity = logoutFinished) }
            }
        }.onFailure {
            Timber.d("Error on logout $it")
        }
    }

    /**
     * Check concurrent calls to see if the call should be switched or ended
     *
     * @param shouldEndCurrentCall if the current call should be finish
     */
    private fun checkAnotherCalls(shouldEndCurrentCall: Boolean) {
        val chatId =
            getCallUseCase.getChatIdOfAnotherCallInProgress(_state.value.chatId).blockingGet()
        if (chatId != MEGACHAT_INVALID_HANDLE && chatId != _state.value.chatId && _switchCall.value != chatId) {
            _switchCall.value = chatId
        } else if (shouldEndCurrentCall) {
            _finishMeetingActivity.value = true
        }
    }

    /**
     * Show the default avatar (the Alphabet avatar)
     */
    private fun showDefaultAvatar() = viewModelScope.launch {
        _avatarLiveData.value = meetingActivityRepository.getDefaultAvatar()
    }

    /**
     * Method to get a specific chat
     *
     * @param chatId Chat ID
     * @return MegaChatRoom
     */
    fun getSpecificChat(chatId: Long): MegaChatRoom? = meetingActivityRepository.getChatRoom(chatId)

    /**
     * Generate and show the round avatar based on the actual avatar stored in the cache folder.
     * Try to retrieve the avatar from the server if it has not been cached.
     * Showing the default avatar if the retrieve failed
     */
    private fun loadAvatar(retry: Boolean = false) {
        viewModelScope.launch {
            meetingActivityRepository.loadAvatar()?.also {
                when {
                    it.first -> _avatarLiveData.value = it.second
                    retry -> meetingActivityRepository.createAvatar(
                        OptionalMegaRequestListenerInterface(onRequestFinish = { request, error ->
                            if (request.type == MegaRequest.TYPE_GET_ATTR_USER
                                && request.paramType == MegaApiJava.USER_ATTR_AVATAR
                                && error.errorCode == MegaError.API_OK
                            ) {
                                loadAvatar()
                            } else {
                                showDefaultAvatar()
                            }
                        })
                    )

                    else -> {
                        showDefaultAvatar()
                    }
                }
            }
        }
    }

    /**
     * Method for update the chatRoom ID
     *
     * @param chatId chat ID
     */
    fun updateChatRoomId(chatId: Long) {
        if (_state.value.chatId != chatId) {
            _state.update {
                it.copy(
                    chatId = chatId
                )
            }
            getChatRoom()
            getChatCall()
            getScheduledMeeting()
            startMonitoringChatParticipantsUpdated(chatId = chatId)
            startMonitorChatRoomUpdates(chatId = chatId)
            startMonitorScheduledMeetingUpdates()
        }
    }

    /**
     * Method to know if the chat exists, I am joined to the chat and the call
     *
     * @return True, if it exists. False, otherwise
     */
    fun isChatCreatedAndIParticipating(): Boolean =
        (_state.value.chatId != MEGACHAT_INVALID_HANDLE &&
                amIParticipatingInAChat(_state.value.chatId) &&
                CallUtil.amIParticipatingInThisMeeting(_state.value.chatId))

    /**
     * Method to initiate the call with the microphone on
     */
    fun micInitiallyOn() {
        Timber.d("Call with audio activated initially")
        _micLiveData.value = true
    }

    /**
     * Method to initiate the call with the camera on
     */
    fun camInitiallyOn() {
        Timber.d("Call with video activated initially")
        _cameraLiveData.value = true
    }

    /**
     * Method for setting a name for the meeting
     *
     * @param name The name
     */
    fun setMeetingsName(name: String) {
        _meetingNameLiveData.value = name
    }

    /**
     * Method for setting a title for the meeting
     *
     * @return The name
     */
    fun getMeetingName(): String? {
        return _meetingNameLiveData.value
    }

    /**
     * Response of clicking mic fab
     *
     * @param shouldAudioBeEnabled True, if audio should be enabled. False, otherwise
     */
    fun clickMic(shouldAudioBeEnabled: Boolean) {
        // Check audio permission. If haven't been granted, ask for the permission and return
        if (!_recordAudioGranted.value) {
            _recordAudioPermissionCheck.value = true
            return
        }

        if (isChatCreatedAndIParticipating()) {
            meetingActivityRepository.switchMic(
                _state.value.chatId,
                shouldAudioBeEnabled,
                DisableAudioVideoCallListener(MegaApplication.getInstance(), this)
            )
        } else {
            //The chat is not yet created or the call is not yet established
            _micLiveData.value = shouldAudioBeEnabled
            Timber.d("open Mic: $shouldAudioBeEnabled")
            tips.value = if (shouldAudioBeEnabled) {
                context.getString(R.string.general_mic_unmute)
            } else {
                context.getString(R.string.general_mic_mute)
            }
        }
    }

    /**
     * Response of clicking camera Fab
     *
     * @param shouldVideoBeEnabled True, if video should be enabled. False, otherwise
     */
    fun clickCamera(shouldVideoBeEnabled: Boolean) {
        //Check camera permission. If haven't been granted, ask for the permission and return
        if (!_cameraGranted.value) {
            _cameraPermissionCheck.value = true
            return
        }

        if (isChatCreatedAndIParticipating()) {
            Timber.d("Clicked cam with chat")
            meetingActivityRepository.switchCamera(
                _state.value.chatId,
                shouldVideoBeEnabled,
                DisableAudioVideoCallListener(MegaApplication.getInstance(), this)
            )
        } else {
            Timber.d("Clicked cam without chat")
            //The chat is not yet created or the call is not yet established
            meetingActivityRepository.switchCameraBeforeStartMeeting(
                shouldVideoBeEnabled,
                OpenVideoDeviceListener(MegaApplication.getInstance(), this)
            )
        }
    }

    /**
     * Method to release the local video device because of the chat is not yet created
     * or the call is not yet established
     */
    fun releaseVideoDevice() {
        meetingActivityRepository.switchCameraBeforeStartMeeting(
            false,
            OpenVideoDeviceListener(MegaApplication.getInstance())
        )
    }

    /**
     * init RTC Audio Manager
     */
    fun initRTCAudioManager() {
        MegaApplication.getInstance()
            .createOrUpdateAudioManager(true, AUDIO_MANAGER_CREATING_JOINING_MEETING)
    }

    /**
     * Response of clicking Speaker Fab
     */
    fun clickSpeaker() {
        when (_speakerLiveData.value) {
            AppRTCAudioManager.AudioDevice.SPEAKER_PHONE -> {
                Timber.d("Trying to switch to EARPIECE")
                meetingActivityRepository.switchSpeaker(AppRTCAudioManager.AudioDevice.EARPIECE)
            }

            else -> {
                Timber.d("Trying to switch to SPEAKER_PHONE")
                meetingActivityRepository.switchSpeaker(AppRTCAudioManager.AudioDevice.SPEAKER_PHONE)
            }
        }
    }

    /**
     * Set camera permission
     *
     * @param cameraPermission true: the permission is granted
     */
    fun setCameraPermission(cameraPermission: Boolean) {
        if (_cameraGranted.value == cameraPermission)
            return

        _cameraGranted.value = cameraPermission
    }

    /**
     * Set record & audio permission
     *
     * @param recordAudioPermission true: the permission is granted
     */
    fun setRecordAudioPermission(recordAudioPermission: Boolean) {
        if (_recordAudioGranted.value == recordAudioPermission)
            return

        _recordAudioGranted.value = recordAudioPermission
    }

    /**
     * Method of obtaining the video
     *
     * @param chatId chatId
     * @param listener IndividualCallVideoListener
     */
    fun addLocalVideo(chatId: Long, listener: IndividualCallVideoListener?) {
        if (listener == null)
            return

        Timber.d("Adding local video")
        meetingActivityRepository.addLocalVideo(chatId, listener)
    }

    /**
     * Method of remove the local video
     *
     * @param chatId chatId
     * @param listener IndividualCallVideoListener
     */
    fun removeLocalVideo(chatId: Long, listener: IndividualCallVideoListener?) {
        if (listener == null) {
            Timber.e("Listener is null")
            return
        }

        Timber.d("Removing local video")
        meetingActivityRepository.removeLocalVideo(chatId, listener)
    }

    /**
     *  Select the video device to be used in calls
     *
     *  @param listener Receive information about requests.
     */
    fun setChatVideoInDevice(listener: MegaChatRequestListenerInterface?) {
        // Always try to start the video using the front camera
        VideoCaptureUtils.getFrontCamera()?.let { cameraDevice ->
            meetingActivityRepository.setChatVideoInDevice(cameraDevice, listener)
        }
    }

    /**
     * Method to update the status of the local camera and display the corresponding tips
     *
     * @param isVideoOn True, if the video is ON. False, otherwise
     */
    private fun updateCameraValueAndTips(isVideoOn: Boolean) {
        _cameraLiveData.value = isVideoOn
        Timber.d("Open video: ${_cameraLiveData.value}")
        tips.value = when (isVideoOn) {
            true -> context.getString(
                R.string.general_camera_enable
            )

            false -> context.getString(
                R.string.general_camera_disable
            )
        }
    }

    override fun onVideoDeviceOpened(isVideoOn: Boolean) {
        updateCameraValueAndTips(isVideoOn)
    }

    override fun onDisableAudioVideo(chatId: Long, typeChange: Int, isEnable: Boolean) {
        when (typeChange) {
            MegaChatRequest.VIDEO -> {
                updateCameraValueAndTips(isEnable)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()

        LiveEventBus.get(EVENT_AUDIO_OUTPUT_CHANGE, AppRTCAudioManager.AudioDevice::class.java)
            .removeObserver(audioOutputStateObserver)

        LiveEventBus.get(
            EVENT_CHAT_TITLE_CHANGE, MegaChatRoom::class.java
        ).removeObserver(titleMeetingChangeObserver)

        LiveEventBus.get(EVENT_MEETING_CREATED, Long::class.java)
            .removeObserver(meetingCreatedObserver)
    }

    fun inviteToChat(context: Context, requestCode: Int, resultCode: Int, intent: Intent?) {
        Timber.d("Result Code: $resultCode")
        if (intent == null) {
            Timber.w("Intent is null")
            return
        }
        if (requestCode == REQUEST_ADD_PARTICIPANTS && resultCode == RESULT_OK) {
            Timber.d("Participants successfully added")
            val contactsData: List<String>? =
                intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
            if (contactsData != null) {
                InviteToChatRoomListener(context).inviteToChat(_state.value.chatId, contactsData)
                _snackBarLiveData.value = context.getString(R.string.invite_sent)

                val newInvitedParticipantsList = state.value.newInvitedParticipants.toMutableList()
                for (contact in contactsData.indices) {
                    newInvitedParticipantsList.add(contactsData[contact])
                }
                _state.update { state ->
                    state.copy(newInvitedParticipants = newInvitedParticipantsList)
                }
            }
        } else {
            Timber.e("Error adding participants")
        }
    }

    /**
     * Show snack bar
     *
     * @param content the content should be shown
     */
    fun showSnackBar(content: String) {
        _snackBarLiveData.value = content
    }

    /**
     * Hide snack bar
     */
    fun hideSnackBar() {
        _snackBarLiveData.value = ""
    }

    /**
     * Method for obtaining the bitmap of a participant's avatar
     *
     * @param peerId User handle of a participant
     * @return The bitmap of a participant's avatar
     */
    fun getAvatarBitmapByPeerId(peerId: Long): Bitmap? {
        return meetingActivityRepository.getAvatarBitmapByPeerId(peerId)
    }

    /**
     * Change permissions to a call participant.
     *
     * @param userHandle User handle of a participant
     * @param permission type of permit to be assigned to the participant
     * @param listener MegaChatRequestListenerInterface
     */
    fun changeParticipantPermissions(
        userHandle: Long,
        permission: Int,
        listener: MegaChatRequestListenerInterface? = null,
    ) {
        meetingActivityRepository.changeParticipantPermissions(
            _state.value.chatId,
            userHandle,
            permission,
            listener
        )
    }

    /**
     * Answer chat call
     *
     * @param enableVideo The video should be enabled
     * @param enableAudio The audio should be enabled
     * @param speakerAudio The speaker should be enabled
     * @return Result of the call
     */
    fun answerCall(
        enableVideo: Boolean,
        enableAudio: Boolean,
        speakerAudio: Boolean,
    ): LiveData<AnswerCallResult> {
        val result = MutableLiveData<AnswerCallResult>()
        _state.value.chatId.let { chatId ->
            if (CallUtil.amIParticipatingInThisMeeting(chatId)) {
                Timber.d("Already participating in this call")
                return result
            }

            if (MegaApplication.getChatManagement().isAlreadyJoiningCall(chatId)) {
                Timber.d("The call has been answered")
                return result
            }

            chatManagement.addJoiningCallChatId(chatId)

            viewModelScope.launch {
                runCatching {
                    setChatVideoInDeviceUseCase()
                    answerChatCallUseCase(chatId = chatId, video = enableVideo, audio = enableAudio)
                }.onSuccess { call ->
                    chatManagement.removeJoiningCallChatId(chatId)
                    rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                    if (call == null) {
                        _finishMeetingActivity.value = true
                    } else {
                        chatManagement.setSpeakerStatus(call.chatId, call.hasLocalVideo)
                        chatManagement.setRequestSentCall(call.callId, false)
                        CallUtil.clearIncomingCallNotification(call.callId)

                        result.value =
                            AnswerCallResult(chatId, call.hasLocalVideo, call.hasLocalAudio)
                    }
                }.onFailure { Timber.w("Exception answering call: $it") }
            }
        }

        return result
    }

    /**
     * Check if the call exists
     */
    fun checkIfCallExists(link: String) {
        viewModelScope.launch {
            runCatching {
                checkChatLink(link)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { request ->
                _state.update {
                    it.copy(isMeetingEnded = isMeetingEnded(request.handleList))
                }
            }
        }
    }

    /**
     * Method to know if a meeting has ended
     *
     * @param list MegaHandleList with the call ID
     * @return True, if the meeting is finished. False, if not.
     */
    fun isMeetingEnded(list: List<Long>?): Boolean =
        list == null || list[0] == MEGACHAT_INVALID_HANDLE

    /**
     * Method to finish the [MeetingActivity]
     */
    fun finishMeetingActivity() {
        _finishMeetingActivity.value = true
    }

    /**
     * Get chat room updates
     *
     * @param chatId Chat id.
     */
    private fun startMonitorChatRoomUpdates(chatId: Long) =
        viewModelScope.launch {
            monitorChatRoomUpdates(chatId).collectLatest { chat ->
                _state.update { state ->
                    with(state) {
                        val permissionValue = if (chat.hasChanged(ChatRoomChange.OwnPrivilege)) {
                            Timber.d("Changes in own privilege")
                            chat.ownPrivilege
                        } else {
                            myPermission
                        }

                        val openInviteValue = if (chat.hasChanged(ChatRoomChange.OpenInvite)) {
                            Timber.d("Changes in OpenInvite")

                            chat.isOpenInvite || myPermission == ChatRoomPermission.Moderator
                        } else {
                            isOpenInvite
                        }

                        val waitingRoomValue = if (chat.hasChanged(ChatRoomChange.WaitingRoom)) {
                            Timber.d("Changes in waiting room")
                            chat.isWaitingRoom
                        } else {
                            hasWaitingRoom
                        }

                        val titleValue = if (chat.hasChanged(ChatRoomChange.Title)) {
                            Timber.d("Changes in title")
                            chat.title
                        } else {
                            title
                        }

                        copy(
                            myPermission = permissionValue,
                            isOpenInvite = openInviteValue,
                            enabledAllowNonHostAddParticipantsOption = chat.isOpenInvite,
                            hasWaitingRoom = waitingRoomValue,
                            title = titleValue,
                            callType = when {
                                chat.isMeeting -> CallType.Meeting
                                !chat.isMeeting && chat.isGroup -> CallType.Group
                                else -> CallType.OneToOne
                            }
                        )
                    }
                }
            }
        }

    /**
     * See waiting room participant list
     */
    fun onSeeAllClick() =
        _state.update { state ->
            state.copy(
                shouldWaitingRoomListBeShown = state.participantsSection == ParticipantsSection.WaitingRoomSection,
                shouldInCallListBeShown = state.participantsSection == ParticipantsSection.InCallSection,
                shouldNotInCallListBeShown = state.participantsSection == ParticipantsSection.NotInCallSection,
            )
        }

    /**
     * More options button clicked
     *
     * @param chatParticipant   [ChatParticipant]
     */
    fun onParticipantMoreOptionsClick(chatParticipant: ChatParticipant?) = _state.update { state ->
        state.copy(
            chatParticipantSelected = chatParticipant,
            selectParticipantEvent = triggered
        )
    }

    /**
     * Consume select participant event
     */
    fun onConsumeSelectParticipantEvent() =
        _state.update { state -> state.copy(selectParticipantEvent = consumed) }

    /**
     * Query meeting link
     *
     * @param shouldShareMeetingLink   True, if link doesn't exist, create it to share it. False, otherwise.
     */
    fun queryMeetingLink(shouldShareMeetingLink: Boolean) {
        if (state.value.meetingLink.isNotEmpty()) {
            _state.update { state ->
                state.copy(
                    shouldShareMeetingLink = shouldShareMeetingLink,
                )
            }
        } else {
            _state.value.chatId.let { id ->
                viewModelScope.launch {
                    runCatching {
                        queryChatLink(id)
                    }.onFailure {
                        if (state.value.hasHostPermission() && shouldShareMeetingLink) {
                            createChatLink()
                        }
                    }.onSuccess { request ->
                        Timber.d("Query chat link successfully")
                        updateMeetingLink(request.text ?: "")
                        _state.update { state ->
                            state.copy(
                                shouldShareMeetingLink = shouldShareMeetingLink,
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * Create chat link
     */
    private fun createChatLink() {
        _state.value.chatId.let { id ->
            viewModelScope.launch {
                runCatching {
                    createChatLink(id)
                }.onFailure { exception ->
                    Timber.e(exception)
                }.onSuccess { request ->
                    Timber.d("Query chat link successfully")
                    updateMeetingLink(request.text ?: "")
                    _state.update { state ->
                        state.copy(
                            shouldShareMeetingLink = true,
                        )
                    }
                }
            }
        }
    }

    /**
     * Update meeting link
     *
     * @param link  New meeting link
     */
    fun updateMeetingLink(link: String) = _state.update { state ->
        state.copy(
            meetingLink = link
        )
    }

    /**
     * Set if bottom panel is expanded
     *
     * @param isExpanded    True if it's expanded. False, if it's collapsed.
     */
    fun setBottomPanelExpanded(isExpanded: Boolean) = _state.update { state ->
        state.copy(
            isBottomPanelExpanded = isExpanded
        )
    }

    /**
     * Sets shouldWaitingRoomListBeShown as consumed.
     */
    fun onConsumeShouldWaitingRoomListBeShownEvent() = _state.update { state ->
        state.copy(shouldWaitingRoomListBeShown = false)
    }

    /**
     * Sets shouldInCallListBeShown as consumed.
     */
    fun onConsumeShouldInCallListBeShownEvent() = _state.update { state ->
        state.copy(shouldInCallListBeShown = false)
    }

    /**
     * Sets shouldNotInCallListBeShown as consumed.
     */
    fun onConsumeShouldNotInCallListBeShownEvent() = _state.update { state ->
        state.copy(shouldNotInCallListBeShown = false)
    }

    /**
     * Sets shouldShareMeetingLink as consumed.
     */
    fun onConsumeShouldShareMeetingLinkEvent() = _state.update { state ->
        state.copy(shouldShareMeetingLink = false)
    }

    /**
     * Allow o deny non-hosts add participants to the call
     */
    fun allowAddParticipantsClick() {
        Timber.d("Update option Allow non-host add participants to the chat room")
        _state.update { state ->
            state.copy(
                enabledAllowNonHostAddParticipantsOption = !state.enabledAllowNonHostAddParticipantsOption,
            )
        }
        viewModelScope.launch {
            runCatching {
                setOpenInvite(state.value.chatId)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { isAllowAddParticipantsEnabled ->
                _state.update { state ->
                    state.copy(
                        isOpenInvite = isAllowAddParticipantsEnabled || state.hasHostPermission(),
                        enabledAllowNonHostAddParticipantsOption = isAllowAddParticipantsEnabled,
                    )
                }
            }
        }
    }

    /**
     * Update Participants selection
     *
     * @param newSelection  New [ParticipantsSection]
     */
    fun updateParticipantsSection(newSelection: ParticipantsSection) =
        _state.update { state ->
            state.copy(
                participantsSection = newSelection
            )
        }

    /**
     * Load all chat participants
     */
    private fun startMonitoringChatParticipantsUpdated(chatId: Long) = viewModelScope.launch {
        runCatching {
            getChatParticipants(chatId)
                .catch { exception ->
                    Timber.e(exception)
                }
                .collectLatest { list ->
                    Timber.d("Updated list of participants: list ${list.size}")
                    _state.update { state ->
                        state.copy(
                            chatParticipantList = list
                        )
                    }

                    checkParticipantLists()
                }
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Check if the call is being recorded
     */
    private fun checkIfCallIsBeingRecorded() {
        viewModelScope.launch {
            runCatching {
                getChatCallUseCase(_state.value.chatId)
            }.onSuccess { call ->
                call?.let {
                    call.sessionByClientId.forEach {
                        it.value.let { session ->
                            if (session.isRecording) {
                                _state.update { state ->
                                    state.copy(
                                        isSessionOnRecording = true,
                                        showRecordingConsentDialog = !state.isRecordingConsentAccepted,
                                        startOrStopRecordingParticipantName = if (state.callType == CallType.OneToOne) {
                                            state.title
                                        } else {
                                            state.usersInCall.find { participant -> participant.peerId == session.peerId }?.name.orEmpty()
                                        }
                                    )
                                }
                                return@forEach
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
     * Update participants in call
     */
    fun updateChatParticipantsInCall(participants: List<Participant>) {
        _state.update { state ->
            state.copy(
                usersInCall = participants
            )
        }
        checkParticipantLists()
        if (!_state.value.isSessionOnRecording) {
            checkIfCallIsBeingRecorded()
        }
    }

    /**
     * Check participant lists
     */
    private fun checkParticipantLists() {
        val chatParticipantsInWaitingRoom = mutableListOf<ChatParticipant>()
        val chatParticipantsInCall = mutableListOf<ChatParticipant>()
        val chatParticipantsNotInCall = state.value.chatParticipantsNotInCall.toMutableList()
        val chatNewInvitedParticipants = state.value.newInvitedParticipants.toMutableList()

        state.value.chatParticipantList.forEach { chatParticipant ->
            var participantAdded = false
            state.value.usersInWaitingRoomIDs.find { it == chatParticipant.handle }?.let {
                participantAdded = true
                chatParticipantsInWaitingRoom.add(chatParticipant)
            }
            state.value.usersInCall.find { it.peerId == chatParticipant.handle }
                ?.let { participant ->
                    participantAdded = true
                    chatParticipantsInCall.add(chatParticipantMapper(participant, chatParticipant))
                }

            chatParticipantsNotInCall.find { it.handle == chatParticipant.handle }
                ?.let { participant ->
                    if (participantAdded) {
                        chatParticipantsNotInCall.remove(participant)
                    }
                } ?: run {
                if (!participantAdded) {
                    chatNewInvitedParticipants.find { it == chatParticipant.email }
                        ?.let { participant ->
                            ringIndividualInACall(chatParticipant.handle)
                            chatNewInvitedParticipants.remove(participant)
                        }
                    chatParticipantsNotInCall.add(chatParticipant)
                }
            }
        }

        _state.update { state ->
            state.copy(
                chatParticipantsInWaitingRoom = chatParticipantsInWaitingRoom,
                chatParticipantsInCall = chatParticipantsInCall,
                chatParticipantsNotInCall = chatParticipantsNotInCall,
            )
        }

        val newSelection =
            if (state.value.usersInWaitingRoomIDs.isEmpty() && state.value.participantsSection == ParticipantsSection.WaitingRoomSection) ParticipantsSection.InCallSection else state.value.participantsSection
        updateParticipantsSection(newSelection)
    }

    /**
     * Add contact
     */
    fun onAddContactClick() {
        state.value.chatParticipantSelected?.let { participant ->
            viewModelScope.launch {
                runCatching {
                    inviteContactUseCase(
                        participant.email ?: "",
                        participant.handle ?: -1,
                        null
                    )
                }.onSuccess { sentInviteResult ->
                    val text = when (sentInviteResult) {
                        InviteContactRequest.Sent -> context.getString(
                            R.string.context_contact_request_sent,
                            participant.email
                        )

                        InviteContactRequest.Resent -> context.getString(R.string.context_contact_invitation_resent)
                        InviteContactRequest.Deleted -> context.getString(R.string.context_contact_invitation_deleted)
                        InviteContactRequest.AlreadySent -> context.getString(
                            R.string.invite_not_sent_already_sent,
                            participant.email
                        )

                        InviteContactRequest.AlreadyContact -> context.getString(
                            R.string.context_contact_already_exists,
                            participant.email
                        )

                        InviteContactRequest.InvalidEmail -> context.getString(R.string.error_own_email_as_contact)
                        InviteContactRequest.InvalidStatus -> context.getString(R.string.general_error)
                    }

                    showSnackBar(text)
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }


    /**
     * Pin to speaker view
     */
    fun onPinToSpeakerView(shouldPinToSpeakerView: Boolean) =
        _state.update { state ->
            state.copy(
                shouldPinToSpeakerView = shouldPinToSpeakerView,
            )
        }

    /**
     * Show or hide remove participant dialog
     */
    fun showOrHideRemoveParticipantDialog(shouldShowDialog: Boolean) =
        _state.update { state ->
            state.copy(
                removeParticipantDialog = shouldShowDialog,
            )
        }

    /**
     * Update participant permissions
     *
     * @param permission [ChatRoomPermission]
     */
    fun updateParticipantPermissions(permission: ChatRoomPermission) =
        _state.value.chatParticipantSelected?.let { participant ->
            viewModelScope.launch {
                runCatching {
                    updateChatPermissionsUseCase(state.value.chatId, participant.handle, permission)
                }.onFailure { exception ->
                    Timber.e(exception)
                }.onSuccess {}
            }
        }

    /**
     * Remove participant from chat
     */
    fun removeParticipantFromChat() =
        _state.value.chatParticipantSelected?.let { participant ->
            viewModelScope.launch {
                runCatching {
                    removeFromChaUseCase(state.value.chatId, participant.handle)
                }.onFailure { exception ->
                    Timber.e(exception)
                }.onSuccess {
                    showOrHideRemoveParticipantDialog(shouldShowDialog = false)
                }
            }
        }

    /**
     * Set if it's speaker mode
     *
     * @param isSpeakerMode True, it's speaker view. False, it's grid view.
     */
    fun setSpeakerView(isSpeakerMode: Boolean) =
        _state.update { it.copy(isSpeakerMode = isSpeakerMode) }

    /**
     * Method handles sent message to chat click from UI
     *
     * returns if user is not online
     * updates [ContactInfoState.isStorageOverQuota] if storage state is [StorageState.PayWall]
     * creates chatroom exists else returns existing chat room
     * updates [ContactInfoState.shouldNavigateToChat] to true
     */
    fun sendMessageToChat() = viewModelScope.launch {
        if (!isOnline()) return@launch
        if (getStorageState() != StorageState.PayWall) {
            startConversation()
        }
    }

    /**
     * Start conversation with participant selected
     */
    private suspend fun startConversation() {
        _state.value.chatParticipantSelected?.let { participant ->
            runCatching {
                startConversationUseCase(isGroup = false, userHandles = listOf(participant.handle))
            }.onSuccess {
                _state.update { state ->
                    state.copy(chatIdToOpen = it)
                }
            }.onFailure {
                showSnackBar(context.getString(R.string.create_chat_error))
            }
        }
    }

    /**
     * on Consume navigate to chat activity event
     */
    fun onConsumeNavigateToChatEvent() =
        _state.update { it.copy(chatIdToOpen = INVALID_CHAT_HANDLE) }

    /**
     * Sets isSessionOnRecording value.
     *
     * @param value Value to set.
     */
    fun setIsSessionOnRecording(value: Boolean) =
        _state.update { state -> state.copy(isSessionOnRecording = value) }

    /**
     * Sets showRecordingConsentDialog as consumed.
     */
    fun setShowRecordingConsentDialogConsumed() =
        _state.update { state -> state.copy(showRecordingConsentDialog = false) }

    /**
     * Sets isRecordingConsentAccepted.
     */
    fun setIsRecordingConsentAccepted(value: Boolean) {
        _state.update { state -> state.copy(isRecordingConsentAccepted = value) }
        launchBroadcastCallRecordingConsentEvent(isRecordingConsentAccepted = value)
    }

    /**
     * Method tod be called when the meeting has started ringing all absent participants
     */
    fun meetingStartedRingingAll() = viewModelScope.launch {
        _state.update { state -> state.copy(isRingingAll = true) }
        delay(TimeUnit.SECONDS.toMillis(DEFAULT_RING_TIMEOUT_SECONDS))
        state.value.chatParticipantsNotInCall.forEach { chatParticipant ->
            updateNotInCallParticipantStatus(
                userId = chatParticipant.handle,
                status = MeetingParticipantNotInCallStatus.NoResponse
            )
        }
        _state.update { state -> state.copy(isRingingAll = false) }
    }

    /**
     * Hang chat call
     */
    fun hangChatCall() = viewModelScope.launch {
        runCatching {
            getChatCallUseCase(_state.value.chatId)?.let { chatCall ->
                hangChatCallUseCase(chatCall.callId)
            }
        }.onSuccess {
            resetCallRecordingState()
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Reset call recording status properties
     */
    fun resetCallRecordingState() {
        _state.update {
            it.copy(
                isSessionOnRecording = false,
                showRecordingConsentDialog = false,
                isRecordingConsentAccepted = false
            )
        }
    }

    /**
     * Set startOrStopRecordingParticipantName as consumed.
     */
    fun setStartOrStopRecordingParticipantNameConsumed() =
        _state.update { state -> state.copy(startOrStopRecordingParticipantName = null) }

    /**
     * Launch broadcast for recording consent event (accepted/rejected)
     *
     * @param isRecordingConsentAccepted True if recording consent has been accepted or False otherwise.
     */
    private fun launchBroadcastCallRecordingConsentEvent(isRecordingConsentAccepted: Boolean) =
        viewModelScope.launch {
            broadcastCallRecordingConsentEventUseCase(isRecordingConsentAccepted)
        }

    /**
     * Ring a participant in chatroom with an ongoing call that they didn't pick up
     * If waiting room is enabled, it also allows the called participant to bypass it (will be future managed by API/SFU/chatd)
     *
     * @param userId   The chat participant ID
     */
    fun ringParticipant(userId: Long) = viewModelScope.launch {
        if (state.value.hasWaitingRoom) {
            runCatching {
                allowUsersJoinCallUseCase(
                    chatId = state.value.chatId, userList = listOf(userId), all = false
                )
            }.onSuccess {
                ringIndividualInACall(userId = userId)
            }.onFailure { exception ->
                Timber.e(exception)
            }
        } else {
            ringIndividualInACall(userId = userId)
        }
    }

    /**
     * Ring a participant in chatroom with an ongoing call that they didn't pick up
     *
     * @param userId   The chat participant ID
     */
    private fun ringIndividualInACall(userId: Long) = viewModelScope.launch {
        runCatching {
            ringIndividualInACallUseCase(chatId = state.value.chatId, userId = userId)
        }.onSuccess {
            updateNotInCallParticipantStatus(
                userId = userId, status = MeetingParticipantNotInCallStatus.Calling
            )
            delay(TimeUnit.SECONDS.toMillis(DEFAULT_RING_TIMEOUT_SECONDS))
            updateNotInCallParticipantStatus(
                userId = userId, status = MeetingParticipantNotInCallStatus.NoResponse
            )
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Ring all absents participants
     */
    fun ringAllAbsentsParticipants() = viewModelScope.launch {
        _state.update {
            it.copy(isRingingAll = true)
        }
        state.value.chatParticipantsNotInCall.forEach { chatParticipant ->
            if (chatParticipant.callStatus != MeetingParticipantNotInCallStatus.Calling) {
                ringParticipant(chatParticipant.handle)
            }
        }
        delay(TimeUnit.SECONDS.toMillis(DEFAULT_RING_TIMEOUT_SECONDS))
        _state.update {
            it.copy(isRingingAll = false)
        }
    }

    /**
     * Update the status of a not in call participant
     *
     * @param userId    The not in call participant ID
     * @param status    New [MeetingParticipantNotInCallStatus]
     */
    private fun updateNotInCallParticipantStatus(
        userId: Long,
        status: MeetingParticipantNotInCallStatus,
    ) {
        val index = state.value.chatParticipantsNotInCall.indexOfFirst { it.handle == userId }
        if (index != INVALID_POSITION) {
            val updatedParticipant =
                state.value.chatParticipantsNotInCall[index].copy(callStatus = status)
            val updatedList = state.value.chatParticipantsNotInCall.updateItemAt(
                index, updatedParticipant
            )
            _state.update {
                it.copy(chatParticipantsNotInCall = updatedList)
            }
        }
    }

    companion object {
        private const val INVALID_CHAT_HANDLE = -1L
        private const val INVALID_POSITION = -1
        private const val DEFAULT_RING_TIMEOUT_SECONDS = 40L
    }
}