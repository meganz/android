package mega.privacy.android.app.meeting.activity

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.extensions.updateItemAt
import mega.privacy.android.app.featuretoggle.ApiFeatures
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.globalmanagement.MegaChatRequestHandler
import mega.privacy.android.app.listeners.InviteToChatRoomListener
import mega.privacy.android.app.listeners.OptionalMegaRequestListenerInterface
import mega.privacy.android.app.main.AddContactActivity
import mega.privacy.android.app.meeting.adapter.Participant
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.meeting.listeners.IndividualCallVideoListener
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.chat.model.AnswerCallResult
import mega.privacy.android.app.presentation.contactinfo.model.ContactInfoUiState
import mega.privacy.android.app.presentation.extensions.getState
import mega.privacy.android.app.presentation.mapper.GetPluralStringFromStringResMapper
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.mapper.ChatParticipantMapper
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil.amIParticipatingInAChat
import mega.privacy.android.app.utils.Constants.AUDIO_MANAGER_CREATING_JOINING_MEETING
import mega.privacy.android.app.utils.Constants.REQUEST_ADD_PARTICIPANTS
import mega.privacy.android.app.utils.RunOnUIThreadUtils.runDelay
import mega.privacy.android.app.utils.VideoCaptureUtils
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.ChatRequestParamType
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.domain.entity.call.CallType
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatCallTermCodeType
import mega.privacy.android.domain.entity.call.ChatSessionChanges
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.meeting.MeetingParticipantNotInCallStatus
import mega.privacy.android.domain.entity.meeting.ParticipantsSection
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.CheckChatLinkUseCase
import mega.privacy.android.domain.usecase.GetChatParticipants
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.QueryChatLinkUseCase
import mega.privacy.android.domain.usecase.RemoveFromChat
import mega.privacy.android.domain.usecase.SetOpenInviteWithChatIdUseCase
import mega.privacy.android.domain.usecase.account.GetCurrentSubscriptionPlanUseCase
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.avatar.GetUserAvatarUseCase
import mega.privacy.android.domain.usecase.call.AllowUsersJoinCallUseCase
import mega.privacy.android.domain.usecase.call.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.call.BroadcastCallEndedUseCase
import mega.privacy.android.domain.usecase.call.CreateMeetingUseCase
import mega.privacy.android.domain.usecase.call.GetCallIdsOfOthersCallsUseCase
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.call.HangChatCallUseCase
import mega.privacy.android.domain.usecase.call.MonitorCallEndedUseCase
import mega.privacy.android.domain.usecase.call.RingIndividualInACallUseCase
import mega.privacy.android.domain.usecase.call.StartCallUseCase
import mega.privacy.android.domain.usecase.chat.CreateChatLinkUseCase
import mega.privacy.android.domain.usecase.chat.IsEphemeralPlusPlusUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatConnectionStateUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRoomUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.chat.UpdateChatPermissionsUseCase
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithHandleUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.BroadcastCallScreenOpenedUseCase
import mega.privacy.android.domain.usecase.meeting.EnableOrDisableAudioUseCase
import mega.privacy.android.domain.usecase.meeting.EnableOrDisableVideoUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChatUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorAudioOutputUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatSessionUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MuteAllPeersUseCase
import mega.privacy.android.domain.usecase.meeting.MutePeersUseCase
import mega.privacy.android.domain.usecase.meeting.StartVideoDeviceUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import nz.mega.sdk.MegaApiJava
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
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
 * @property rtcAudioManagerGateway                         [RTCAudioManagerGateway]
 * @property getChatParticipants                            [GetChatParticipants]
 * @property chatManagement                                 [ChatManagement]
 * @property setChatVideoInDeviceUseCase                    [SetChatVideoInDeviceUseCase]
 * @property checkChatLink                                  [CheckChatLinkUseCase]
 * @property monitorChatCallUpdatesUseCase                  [MonitorChatCallUpdatesUseCase]
 * @property getChatRoomUseCase                             [GetChatRoomUseCase]
 * @property getChatCallUseCase                             [GetChatCallUseCase]
 * @property setOpenInviteWithChatIdUseCase                 [SetOpenInviteWithChatIdUseCase]
 * @property chatParticipantMapper                          [ChatParticipantMapper]
 * @property monitorChatRoomUpdatesUseCase                  [MonitorChatRoomUpdatesUseCase]
 * @property queryChatLinkUseCase                           [QueryChatLinkUseCase]
 * @property isEphemeralPlusPlusUseCase                     [IsEphemeralPlusPlusUseCase]
 * @property createChatLinkUseCase                          [CreateChatLinkUseCase]
 * @property inviteContactWithHandleUseCase                           [InviteContactWithHandleUseCase]
 * @property updateChatPermissionsUseCase                   [UpdateChatPermissionsUseCase]
 * @property removeFromChaUseCase                           [RemoveFromChat]
 * @property startConversationUseCase                       [StartConversationUseCase]
 * @property isConnectedToInternetUseCase                   [IsConnectedToInternetUseCase]
 * @property monitorStorageStateEventUseCase                [MonitorStorageStateEventUseCase]
 * @property hangChatCallUseCase                            [HangChatCallUseCase]
 * @property broadcastCallEndedUseCase                      [BroadcastCallEndedUseCase]
 * @property broadcastCallScreenOpenedUseCase               [BroadcastCallScreenOpenedUseCase]
 * @property monitorScheduledMeetingUpdatesUseCase          [MonitorScheduledMeetingUpdatesUseCase]
 * @property getMyFullNameUseCase                           [GetMyFullNameUseCase]
 * @property deviceGateway                                  [DeviceGateway]
 * @property monitorUserUpdates                             [MonitorUserUpdates]
 * @property ringIndividualInACallUseCase                   [RingIndividualInACallUseCase]
 * @property allowUsersJoinCallUseCase                      [AllowUsersJoinCallUseCase]
 * @property mutePeersUseCase                               [MutePeersUseCase]
 * @property muteAllPeersUseCase                            [MuteAllPeersUseCase]
 * @property getStringFromStringResMapper                   [GetStringFromStringResMapper]
 * @property getCurrentSubscriptionPlanUseCase              [GetCurrentSubscriptionPlanUseCase]
 * @property monitorAudioOutputUseCase                      [MonitoraAudioOutputUseCase]
 * @property state                                          Current view state as [MeetingState]
 */
@HiltViewModel
class MeetingActivityViewModel @Inject constructor(
    private val meetingActivityRepository: MeetingActivityRepository,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val getCallIdsOfOthersCallsUseCase: GetCallIdsOfOthersCallsUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val chatManagement: ChatManagement,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val checkChatLink: CheckChatLinkUseCase,
    private val getChatParticipants: GetChatParticipants,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val monitorChatSessionUpdatesUseCase: MonitorChatSessionUpdatesUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val monitorChatRoomUpdatesUseCase: MonitorChatRoomUpdatesUseCase,
    private val queryChatLinkUseCase: QueryChatLinkUseCase,
    private val setOpenInviteWithChatIdUseCase: SetOpenInviteWithChatIdUseCase,
    private val chatParticipantMapper: ChatParticipantMapper,
    private val isEphemeralPlusPlusUseCase: IsEphemeralPlusPlusUseCase,
    private val createChatLinkUseCase: CreateChatLinkUseCase,
    private val inviteContactWithHandleUseCase: InviteContactWithHandleUseCase,
    private val updateChatPermissionsUseCase: UpdateChatPermissionsUseCase,
    private val removeFromChaUseCase: RemoveFromChat,
    private val startConversationUseCase: StartConversationUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val broadcastCallEndedUseCase: BroadcastCallEndedUseCase,
    private val broadcastCallScreenOpenedUseCase: BroadcastCallScreenOpenedUseCase,
    private val getScheduledMeetingByChatUseCase: GetScheduledMeetingByChatUseCase,
    private val getMyFullNameUseCase: GetMyFullNameUseCase,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val monitorScheduledMeetingUpdatesUseCase: MonitorScheduledMeetingUpdatesUseCase,
    private val deviceGateway: DeviceGateway,
    private val ringIndividualInACallUseCase: RingIndividualInACallUseCase,
    private val allowUsersJoinCallUseCase: AllowUsersJoinCallUseCase,
    private val mutePeersUseCase: MutePeersUseCase,
    private val muteAllPeersUseCase: MuteAllPeersUseCase,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
    private val getCurrentSubscriptionPlanUseCase: GetCurrentSubscriptionPlanUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val getPluralStringFromStringResMapper: GetPluralStringFromStringResMapper,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
    private val startVideoDeviceUseCase: StartVideoDeviceUseCase,
    private val enableOrDisableVideoUseCase: EnableOrDisableVideoUseCase,
    private val enableOrDisableAudioUseCase: EnableOrDisableAudioUseCase,
    private val monitorCallEndedUseCase: MonitorCallEndedUseCase,
    private val createMeetingUseCase: CreateMeetingUseCase,
    private val startCallUseCase: StartCallUseCase,
    private val passcodeManagement: PasscodeManagement,
    private val monitorAudioOutputUseCase: MonitorAudioOutputUseCase,
    private val monitorChatConnectionStateUseCase: MonitorChatConnectionStateUseCase,
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context,
    private val getUserAvatarUseCase: GetUserAvatarUseCase,
    private val megaChatRequestHandler: MegaChatRequestHandler,
) : ViewModel() {
    private val _state = MutableStateFlow(
        MeetingState(
            chatId = savedStateHandle[MeetingActivity.MEETING_CHAT_ID]
                ?: -1L
        )
    )

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

    private var raisedHandUsersMap: Map<Long, Pair<Boolean, Int>> = emptyMap()

    private var monitorChatCallUpdatesJob: Job? = null
    private var monitorChatConnectionStateJob: Job? = null

    // OnOffFab
    private val _micLiveData: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    private val _cameraLiveData: MutableLiveData<Boolean> = MutableLiveData<Boolean>(false)
    private val _speakerLiveData: MutableLiveData<AudioDevice> =
        MutableLiveData<AudioDevice>().apply {
            val audioManager = rtcAudioManagerGateway.audioManager
            value = audioManager?.selectedAudioDevice ?: AudioDevice.None
        }

    val micLiveData: LiveData<Boolean> = _micLiveData
    val cameraLiveData: LiveData<Boolean> = _cameraLiveData
    val speakerLiveData: LiveData<AudioDevice> = _speakerLiveData

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

    // Show snack bar
    private val _snackBarLiveData = MutableLiveData("")
    val snackBarLiveData: LiveData<String> = _snackBarLiveData

    //Control when call should be switched
    private val _switchCall = MutableStateFlow(MEGACHAT_INVALID_HANDLE)
    val switchCall: StateFlow<Long> get() = _switchCall

    //Control when call should be finish
    private val _finishMeetingActivity = MutableStateFlow(false)
    val finishMeetingActivity: StateFlow<Boolean> get() = _finishMeetingActivity

    /**
     * Set meeting action
     *
     * @param action meeting action type
     */
    fun setAction(action: String?) {
        _state.update { it.copy(action = action) }
    }

    init {
        viewModelScope.launch {
            monitorCallEndedUseCase()
                .catch { Timber.e(it) }
                .collect {
                    if (it == state.value.chatId) {
                        finishMeetingActivity()
                    }
                }
        }
        startMonitoringAudioOutput()

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

        startMonitoringChatCallUpdates()
        startMonitorChatSessionUpdates()
        getMyFullName()
        getMyUserHandle()

        if (_state.value.chatId != -1L) {
            getChatAndCall()
        }

        getCurrentSubscriptionPlan()
        getApiFeatureFlag()

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

    private fun startMonitoringAudioOutput() {
        viewModelScope.launch {
            monitorAudioOutputUseCase()
                .catch { Timber.e(it) }
                .collect {
                    if (_speakerLiveData.value != it && it != AudioDevice.None) {
                        Timber.d("Updating speaker $it")

                        _speakerLiveData.value = it
                        tips.value = when (it) {
                            AudioDevice.Earpiece -> context.getString(R.string.general_speaker_off)
                            AudioDevice.SpeakerPhone -> context.getString(R.string.general_speaker_on)
                            else -> context.getString(R.string.general_headphone_on)
                        }
                    }
                }
        }
    }

    internal fun monitorChatConnectionStatus(chatId: Long) {
        monitorChatConnectionStateJob?.cancel()
        monitorChatConnectionStateJob = viewModelScope.launch {
            monitorChatConnectionStateUseCase()
                .filter { it.chatId == chatId }
                .collectLatest {
                    _state.update { state ->
                        state.copy(chatConnectionStatus = it.chatConnectionStatus)
                    }
                }
        }
    }

    /**
     * Get call unlimited pro plan api feature flag
     */
    private fun getApiFeatureFlag() {
        viewModelScope.launch {
            runCatching {
                getFeatureFlagValueUseCase(ApiFeatures.CallUnlimitedProPlan)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { flag ->
                _state.update { state ->
                    state.copy(
                        isCallUnlimitedProPlanFeatureFlagEnabled = flag,
                    )
                }
            }
        }
    }

    private fun getCurrentSubscriptionPlan() {
        viewModelScope.launch {
            runCatching {
                val subscriptionPlan = getCurrentSubscriptionPlanUseCase()
                subscriptionPlan?.let {
                    _state.update {
                        it.copy(subscriptionPlan = subscriptionPlan)
                    }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Update guest full name and meeting link
     *
     * @param firstName First name of a guest
     * @param lastName Last name of a guest
     * @param meetingLink   Meeting link
     */
    fun updateGuestFullNameAndMeetingLink(
        firstName: String,
        lastName: String,
        meetingLink: String,
        meetingName: String,
    ) =
        _state.update { state ->
            state.copy(
                guestFirstName = firstName,
                guestLastName = lastName,
                meetingLink = meetingLink,
                meetingName = meetingName
            )
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
                    _state.update { state ->
                        state.copy(
                            currentCall = it
                        )
                    }

                    getMyUserHandle()
                    updateParticipantsWithRaisedHand()
                    when (call.status) {
                        ChatCallStatus.UserNoPresent -> {
                            if (_state.value.action == MeetingActivity.MEETING_ACTION_IN) {
                                finishMeetingActivity()
                            }
                        }

                        ChatCallStatus.TerminatingUserParticipation,
                        ChatCallStatus.Destroyed,
                        -> finishMeetingActivity()

                        else -> {
                            checkIfPresenting(it)
                            if (state.value.isRaiseToSpeakFeatureFlagEnabled) {
                                Timber.d("Call recovered, check the participants with raised  hand")
                                initialiseUserToShowInHandRaisedSnackbar(call)
                            }
                            if (checkEphemeralAccount) {
                                checkEphemeralAccountAndWaitingRoom(it)
                            }
                        }
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
                getScheduledMeetingByChatUseCase(state.value.chatId)
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
     * Trigger event to show Snackbar message
     *
     * @param message     Content for snack bar
     */
    private fun triggerHandRaisedSnackbarMsg(message: String) {
        clearUserToShowInHandRaisedSnackbar()
        onSnackbarMessageConsumed()
        _state.update { it.copy(handRaisedSnackbarMsg = triggered(message)) }
    }

    /**
     * Reset and notify that snackbarMessage is consumed
     */
    fun onHandRaisedSnackbarMsgConsumed() {
        _state.update {
            it.copy(handRaisedSnackbarMsg = consumed())
        }
    }

    /**
     * Control the snackbar to be displayed when someone raises their hand.
     */
    private fun checkShowHandRaisedSnackbar() {
        _state.update {
            it.copy(
                showLowerHandButtonInSnackbar = state.value.isMyHandRaisedToShowSnackbar
            )
        }

        when (val numberOfParticipants = state.value.userToShowInHandRaisedSnackbarNumber()) {
            0 -> Timber.d("No users with hand raised")
            1 -> when {
                state.value.isMyHandRaisedToShowSnackbar -> triggerHandRaisedSnackbarMsg(
                    getStringFromStringResMapper(
                        R.string.meeting_your_hand_is_raised_message
                    )
                )

                else -> {
                    val name = state.value.getParticipantNameWithRaisedHand()
                    triggerHandRaisedSnackbarMsg(
                        getStringFromStringResMapper(
                            R.string.meetings_one_participant_raised_their_hand_message,
                            name
                        )
                    )
                }
            }

            else -> when {
                state.value.isMyHandRaisedToShowSnackbar -> triggerHandRaisedSnackbarMsg(
                    getPluralStringFromStringResMapper(
                        stringId = R.plurals.meeting_you_and_others_raised_your_hands_message,
                        quantity = numberOfParticipants - 1,
                        numberOfParticipants - 1
                    )
                )

                else -> state.value.getParticipantNameWithRaisedHand()
                    .let { name ->
                        triggerHandRaisedSnackbarMsg(
                            getPluralStringFromStringResMapper(
                                stringId = R.plurals.meetings_other_participants_raised_their_hands_message,
                                quantity = numberOfParticipants - 1,
                                name,
                                numberOfParticipants - 1
                            )
                        )
                    }
            }
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
    private fun startMonitoringChatCallUpdates() {
        monitorChatCallUpdatesJob?.cancel()
        monitorChatCallUpdatesJob = viewModelScope.launch {
            monitorChatCallUpdatesUseCase()
                .filter { it.chatId == _state.value.chatId }
                .collectLatest { call ->
                    _state.update { state ->
                        state.copy(
                            currentCall = call
                        )
                    }

                    checkIfPresenting(call)
                    call.changes?.apply {
                        when {
                            contains(ChatCallChanges.Status) -> {
                                Timber.d("Chat call status: ${call.status}")
                                when (call.status) {
                                    ChatCallStatus.InProgress -> {
                                        Timber.d("Call in progress, check my user handle and ephemeral account")
                                        getMyUserHandle()
                                        checkEphemeralAccountAndWaitingRoom(call)
                                        if (state.value.isRaiseToSpeakFeatureFlagEnabled) {
                                            Timber.d("Call in progress, check the participants with raised hand")
                                            updateParticipantsWithRaisedHand()
                                            initialiseUserToShowInHandRaisedSnackbar(call)
                                        }
                                    }

                                    ChatCallStatus.TerminatingUserParticipation, ChatCallStatus.GenericNotification -> {
                                        Timber.d("Chat call termCode: ${call.termCode}")
                                        when (call.termCode) {
                                            ChatCallTermCodeType.CallUsersLimit -> _state.update { state ->
                                                state.copy(
                                                    callEndedDueToFreePlanLimits = true
                                                )
                                            }

                                            ChatCallTermCodeType.TooManyParticipants,
                                            ChatCallTermCodeType.TooManyClients,
                                            -> _state.update { state ->
                                                state.copy(
                                                    callEndedDueToTooManyParticipants = true
                                                )
                                            }

                                            else -> if (call.status == ChatCallStatus.TerminatingUserParticipation) {
                                                broadcastCallEndedUseCase(_state.value.chatId)
                                                finishMeetingActivity()
                                            }
                                        }
                                    }

                                    ChatCallStatus.Destroyed -> {
                                        broadcastCallEndedUseCase(_state.value.chatId)
                                        finishMeetingActivity()
                                    }

                                    else -> {}
                                }
                            }

                            contains(ChatCallChanges.CallRaiseHand) -> {
                                if (state.value.isRaiseToSpeakFeatureFlagEnabled) {
                                    when (call.flag) {
                                        true -> {
                                            val listToUpdate =
                                                state.value.userToShowInHandRaisedSnackbar +
                                                        (call.raisedHandsList?.filterNot {
                                                            state.value.userToShowInHandRaisedSnackbar.containsKey(
                                                                it
                                                            )
                                                        }
                                                            ?.associateWith { true } ?: emptyMap())
                                            Timber.d("Change in CallRaiseHand, update participants with raised hand")
                                            updateUserToShowInHandRaisedSnackbar(listToUpdate)
                                            monitorGroupHandRaisedSnackbar()
                                        }

                                        false -> {
                                            val listToUpdate =
                                                state.value.userToShowInHandRaisedSnackbar.filter { entry ->
                                                    call.raisedHandsList?.contains(entry.key)
                                                        ?: false
                                                }.toMap()
                                            Timber.d("Change in CallRaiseHand, update participants with raised hand")
                                            updateUserToShowInHandRaisedSnackbar(listToUpdate)
                                        }
                                    }
                                    updateParticipantsWithRaisedHand()
                                }
                            }

                            contains(ChatCallChanges.LocalAVFlags) -> {
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

                                call.auxHandle?.let { auxClientId ->
                                    if (auxClientId != 0L) {
                                        state.value.usersInCall.find { it.clientId == auxClientId }
                                            ?.let {
                                                showSnackBar(
                                                    context.getString(
                                                        R.string.meetings_muted_by_a_participant_snackbar_message,
                                                        it.name
                                                    )
                                                )
                                            }
                                    }
                                }
                            }

                            contains(ChatCallChanges.WaitingRoomUsersEntered) ||
                                    contains(ChatCallChanges.WaitingRoomUsersLeave) -> {
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
        }
    }

    /**
     * Update chat participants with hand raised list
     */
    private fun updateParticipantsWithRaisedHand() {
        var order = 0
        raisedHandUsersMap = buildMap {
            state.value.currentCall?.usersRaiseHands?.map {
                if (it.value) {
                    put(it.key, Pair(it.value, ++order))
                }
            }
        }
        Timber.d("Raised Hands Map and Order $raisedHandUsersMap")
        checkParticipantLists()
    }

    /**
     * Monitor group snackbar notifications when several users raise their hands at the same time
     */
    private fun monitorGroupHandRaisedSnackbar() {
        if (!state.value.isWaitingForGroupHandRaisedSnackbars) {
            _state.update { state ->
                state.copy(
                    isWaitingForGroupHandRaisedSnackbars = true
                )
            }

            runDelay(DEFAULT_GROUP_SNACKBARS_TIMEOUT_MILLISECONDS) {
                _state.update { state ->
                    state.copy(
                        isWaitingForGroupHandRaisedSnackbars = false
                    )
                }
                checkShowHandRaisedSnackbar()
            }
        }
    }

    /**
     * Send event that in Meeting fragment is visible or not
     *
     * @param isVisible True if the fragment it visible, false otherwise.
     */
    fun sendEnterCallEvent(isVisible: Boolean) = viewModelScope.launch {
        broadcastCallScreenOpenedUseCase(isVisible)
    }

    /**
     * Get chat session updates
     */
    private fun startMonitorChatSessionUpdates() =
        viewModelScope.launch {
            monitorChatSessionUpdatesUseCase()
                .filter { it.call?.chatId == _state.value.chatId }
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
            megaChatRequestHandler.setIsLoggingRunning(true)
        }.onSuccess {
            _state.update { it.copy(shouldLaunchLeftMeetingActivity = true) }
        }.onFailure {
            Timber.d("Error on logout $it")
        }
    }

    /**
     * Show participants list
     *
     * @param shouldBeShown True, should be shown. False, should be hidden.
     */
    fun showParticipantsList(shouldBeShown: Boolean) =
        _state.update { state -> state.copy(shouldParticipantInCallListBeShown = shouldBeShown) }

    /**
     * Check concurrent calls to see if the call should be switched or ended
     *
     * @param shouldEndCurrentCall if the current call should be finish
     */
    private fun checkAnotherCalls(shouldEndCurrentCall: Boolean) {
        viewModelScope.launch {
            val chatId =
                runCatching { getCallIdsOfOthersCallsUseCase(_state.value.chatId).first() }.getOrElse { MEGACHAT_INVALID_HANDLE }
            if (chatId != MEGACHAT_INVALID_HANDLE && chatId != _state.value.chatId && _switchCall.value != chatId) {
                _switchCall.value = chatId
            } else if (shouldEndCurrentCall) {
                finishMeetingActivity()
            }
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
     * Method to start or create a meeting
     *
     * @param title Meeting title
     * @param video True, if the video should be activated. False, if not
     * @param audio True, if the audio should be activated. False, if not
     */
    fun startOrCreateMeeting(title: String, video: Boolean, audio: Boolean) {
        viewModelScope.launch {
            runCatching {
                getChatRoomUseCase(_state.value.chatId)
            }.onSuccess { chatRoom ->
                chatRoom?.let { chat ->
                    Timber.d("The chat exists")
                    if (CallUtil.isStatusConnected(
                            MegaApplication.getInstance().applicationContext,
                            chat.chatId
                        )
                    ) {
                        runCatching {
                            getChatCallUseCase(chat.chatId)
                        }.onSuccess { chatCall ->
                            chatCall?.let { call ->
                                Timber.d("There is a call, open it")
                                CallUtil.openMeetingInProgress(
                                    MegaApplication.getInstance().applicationContext,
                                    call.chatId,
                                    true,
                                    passcodeManagement
                                )
                                _state.update {
                                    it.copy(
                                        startedMeetingChatId = call.chatId
                                    )
                                }
                            } ?: run {
                                Timber.d("Chat status is connected and the call does not exist")
                                setIsWaitingForCall(false)
                                setChatVideoInDevice(
                                    chatId = chat.chatId,
                                    shouldStartMeeting = true,
                                    shouldVideoBeEnabled = video,
                                    shouldAudioBeEnabled = audio
                                )
                            }

                        }.onFailure { exception ->
                            Timber.e(exception)
                        }
                    }
                } ?: run {
                    Timber.d("The chat does not exist, create meeting")
                    createMeeting(title)
                }

            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Create meeting
     *
     * @param title Meeting title
     */
    private fun createMeeting(title: String) {
        viewModelScope.launch {
            runCatching {
                createMeetingUseCase(
                    title = title,
                    speakRequest = false,
                    waitingRoom = false,
                    openInvite = true
                )
            }.onSuccess {
                if (it.number.toInt() == 1) {
                    Timber.d("Meeting created,  chat id ${it.chatHandle}")
                    setIsWaitingForCall(true)
                    updateChatRoomId(it.chatHandle)
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Set if the user is waiting for a call.
     */
    fun setIsWaitingForCall(isWaitingForCall: Boolean) {
        _state.update { state -> state.copy(isWaitingForCall = isWaitingForCall) }
    }

    /**
     * Method for update the chatRoom ID
     *
     * @param chatId chat ID
     */
    fun updateChatRoomId(chatId: Long) {
        if (_state.value.chatId != chatId && chatId != -1L) {
            _state.update {
                it.copy(
                    chatId = chatId
                )
            }

            getChatAndCall()
        }
    }

    /**
     * Get chat and call
     */
    private fun getChatAndCall() {
        getChatRoom()
        getChatCall()
        getScheduledMeeting()
        _state.value.chatId.let { chatId ->
            startMonitoringChatParticipantsUpdated(chatId)
            startMonitorChatRoomUpdates(chatId)
            monitorChatConnectionStatus(chatId)
        }
        startMonitorScheduledMeetingUpdates()
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
     * Response of clicking mic fab
     *
     * @param shouldAudioBeEnabled True, if audio should be enabled. False, otherwise
     */
    fun clickMic(shouldAudioBeEnabled: Boolean) {
        Timber.d("click mic shouldAudioBeEnabled: $shouldAudioBeEnabled")
        // Check audio permission. If haven't been granted, ask for the permission and return
        if (!_recordAudioGranted.value) {
            _recordAudioPermissionCheck.value = true
            return
        }
        when {
            isChatCreatedAndIParticipating() -> enableAudio(enable = shouldAudioBeEnabled)
            else -> {
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

        setChatVideoInDevice(
            shouldStartMeeting = false,
            shouldVideoBeEnabled = shouldVideoBeEnabled,
        )
    }

    /**
     * Set chat video in device
     *
     * @param chatId    Chat id
     * @param shouldStartMeeting    Should start the meeting
     * @param shouldVideoBeEnabled  True, if the video should be activated. False, if not
     * @param shouldAudioBeEnabled  True, if the audio should be activated. False, if not     *
     */
    private fun setChatVideoInDevice(
        chatId: Long = -1,
        shouldStartMeeting: Boolean,
        shouldVideoBeEnabled: Boolean,
        shouldAudioBeEnabled: Boolean = false,
    ) {
        viewModelScope.launch {
            runCatching {
                setChatVideoInDeviceUseCase()
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess {
                when {
                    shouldStartMeeting -> {
                        startMeeting(
                            chatId,
                            enableAudio = shouldAudioBeEnabled,
                            enableVideo = shouldVideoBeEnabled
                        )
                    }

                    isChatCreatedAndIParticipating() -> enableVideo(enable = shouldVideoBeEnabled)
                    else -> enableDeviceCamera(
                        enable = shouldVideoBeEnabled,
                        isReleasingVideo = false
                    )
                }
            }
        }
    }

    /**
     * Start meeting
     *
     * @param chatId        Chat id
     * @param enableAudio   Audio on or off
     * @param enableVideo   Video on or off
     */
    private fun startMeeting(
        chatId: Long,
        enableVideo: Boolean,
        enableAudio: Boolean,
    ) {
        viewModelScope.launch {
            runCatching {
                startCallUseCase(
                    chatId = chatId,
                    audio = enableAudio,
                    video = enableVideo
                )
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { chatCall ->
                chatCall?.apply {
                    chatManagement.setSpeakerStatus(chatId, hasLocalVideo)
                    if (isOutgoing) {
                        chatManagement.setRequestSentCall(callId, isRequestSent = true)
                    }
                    _state.update {
                        it.copy(
                            startedMeetingChatId = chatId
                        )
                    }
                }
            }
        }
    }

    /**
     * Enable device camera
     *
     * @param enable            True to enable camera, false otherwise
     * @param isReleasingVideo  True, video is being released. False, only changes the camera status.
     */
    private fun enableDeviceCamera(enable: Boolean, isReleasingVideo: Boolean) =
        viewModelScope.launch {
            Timber.d("Enable device camera: $enable and isReleasingVideo: $isReleasingVideo")
            runCatching {
                startVideoDeviceUseCase(enable)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { chatRequest ->
                if (!isReleasingVideo) {
                    updateCameraValueAndTips(chatRequest.flag)
                }
            }
        }

    /**
     * Enable video
     *
     * @param enable    true to enable video, false otherwise
     */
    private fun enableVideo(enable: Boolean) {
        state.value.currentCall?.apply {
            Timber.d("Enable video: $enable and has Local Video $hasLocalVideo")
            if (enable && hasLocalVideo) {
                return
            }
            viewModelScope.launch {
                runCatching {
                    enableOrDisableVideoUseCase(chatId = chatId, enable = enable)
                }.onFailure { exception ->
                    Timber.e(exception)
                }.onSuccess { chatRequest ->
                    chatRequest.apply {
                        if (paramType == ChatRequestParamType.Video) {
                            updateCameraValueAndTips(flag)
                        }
                    }
                }
            }
        }
    }

    /**
     * Enable audio
     *
     * @param enable    true to enable audio, false otherwise
     */
    private fun enableAudio(enable: Boolean) {
        state.value.currentCall?.apply {
            Timber.d("Enable audio: $enable and has Local Audio $hasLocalAudio")
            if (enable && hasLocalAudio) {
                return
            }
            viewModelScope.launch {
                runCatching {
                    enableOrDisableAudioUseCase(chatId, enable)
                }.onFailure { exception ->
                    Timber.e(exception)
                }
            }
        }
    }

    /**
     * Method to release the local video device because of the chat is not yet created
     * or the call is not yet established
     */
    fun releaseVideoDevice() {
        enableDeviceCamera(enable = false, isReleasingVideo = true)
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
            AudioDevice.SpeakerPhone -> {
                Timber.d("Trying to switch to EARPIECE")
                meetingActivityRepository.switchSpeaker(AudioDevice.Earpiece)
            }

            else -> {
                Timber.d("Trying to switch to SPEAKER_PHONE")
                meetingActivityRepository.switchSpeaker(AudioDevice.SpeakerPhone)
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
        return meetingActivityRepository.getAvatarBitmapByPeerId(peerId) {
            getRemoteUserAvatar(peerId)
        }
    }

    private fun getRemoteUserAvatar(peerId: Long) {
        viewModelScope.launch {
            runCatching { getUserAvatarUseCase(peerId) }
                .onSuccess {
                    _state.update { state ->
                        state.copy(userAvatarUpdateId = peerId)
                    }
                }
                .onFailure { Timber.d(it) }
        }
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
                        finishMeetingActivity()
                    } else {
                        chatManagement.setSpeakerStatus(call.chatId, speakerAudio)
                        chatManagement.setRequestSentCall(call.callId, false)
                        CallUtil.clearIncomingCallNotification(call.callId)

                        result.value =
                            AnswerCallResult(chatId, call.hasLocalVideo, call.hasLocalAudio)
                    }
                }.onFailure {
                    Timber.w("Exception answering call: $it")
                }
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
    private fun isMeetingEnded(list: List<Long>?): Boolean =
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
            monitorChatRoomUpdatesUseCase(chatId).collectLatest { chat ->
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
                            meetingName = titleValue,
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
                        queryChatLinkUseCase(id)
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
                    createChatLinkUseCase(id)
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
                setOpenInviteWithChatIdUseCase(state.value.chatId)
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
     * Update participants in call
     */
    fun updateChatParticipantsInCall(participants: List<Participant>) {
        _state.update { state ->
            state.copy(
                usersInCall = participants
            )
        }

        checkParticipantLists()
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
                    chatParticipantsInCall.add(
                        chatParticipantMapper(
                            participant,
                            chatParticipant,
                            raisedHandUsersMap[chatParticipant.handle]
                        )
                    )
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


        val sortedChatParticipantsInCall =
            chatParticipantsInCall.sortedBy { it.order }

        _state.update { state ->
            state.copy(
                chatParticipantsInWaitingRoom = chatParticipantsInWaitingRoom,
                chatParticipantsInCall = sortedChatParticipantsInCall,
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
                    inviteContactWithHandleUseCase(
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
     * Initialise user to show in hand raised snackbar
     *
     * @param call  [ChatCall]
     */
    private fun initialiseUserToShowInHandRaisedSnackbar(call: ChatCall) {
        Timber.d("Initially check participants with raised hand")
        val listToUpdate = buildMap {
            call.raisedHandsList?.forEach { peerId ->
                this[peerId] = false
            }
        }

        updateUserToShowInHandRaisedSnackbar(listToUpdate)
    }

    /**
     * Clear user ids with changes in raised hand list
     */
    private fun clearUserToShowInHandRaisedSnackbar() {
        Timber.d("Clear list to update with hand raised")
        val listToUpdate = mutableMapOf<Long, Boolean>()
        state.value.userToShowInHandRaisedSnackbar.forEach {
            listToUpdate[it.key] = false
        }

        updateUserToShowInHandRaisedSnackbar(listToUpdate)
    }

    /**
     * Update user ids with changes in raised hand list
     *
     * @param list  map with new values
     */
    private fun updateUserToShowInHandRaisedSnackbar(list: Map<Long, Boolean>) =
        _state.update { state -> state.copy(userToShowInHandRaisedSnackbar = list) }

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
                    updateChatPermissionsUseCase(
                        chatId = state.value.chatId,
                        nodeId = NodeId(participant.handle),
                        permission = permission
                    )
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
     * updates [ContactInfoUiState.isStorageOverQuota] if storage state is [StorageState.PayWall]
     * creates chatroom exists else returns existing chat room
     * updates [ContactInfoUiState.shouldNavigateToChat] to true
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
     * Mute all participants
     */
    fun muteAllParticipants() {
        viewModelScope.launch {
            runCatching {
                muteAllPeersUseCase(
                    chatId = state.value.chatId
                )
            }.onSuccess {
                showSnackBar(
                    context.getString(
                        R.string.meetings_muted_all_participants_snackbar_message,
                    )
                )
                triggerSnackbarMessage(getStringFromStringResMapper(R.string.meetings_muted_all_participants_snackbar_message))
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Mute one participant
     */
    fun muteParticipant() {
        _state.value.chatParticipantSelected?.let { participant ->
            muteParticipantByClientId(participant.callParticipantData.clientId)
        }
    }

    /**
     * Mute participant
     *
     * @param clientId   Client id of a participant
     */
    fun muteParticipantByClientId(clientId: Long) {
        viewModelScope.launch {
            runCatching {
                mutePeersUseCase(
                    chatId = state.value.chatId, clientId = clientId
                )
            }.onSuccess {
                state.value.usersInCall.find { it.clientId == clientId }?.let {
                    showSnackBar(
                        context.getString(
                            R.string.meetings_muted_a_participant_snackbar_message,
                            it.name
                        )
                    )
                    triggerSnackbarMessage(
                        getStringFromStringResMapper(
                            R.string.meetings_muted_a_participant_snackbar_message,
                            it.name
                        )
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Update PiP mode value
     */
    fun updateIsInPipMode(isInPipMode: Boolean) {
        viewModelScope.launch {
            _state.update {
                it.copy(isInPipMode = isInPipMode)
            }
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

    /**
     * Trigger event to show Snackbar message
     *
     * @param message     Content for snack bar
     */
    private fun triggerSnackbarMessage(message: String) =
        _state.update { it.copy(snackbarMsg = triggered(message)) }

    /**
     * Reset and notify that snackbarMessage is consumed
     */
    fun onSnackbarMessageConsumed() =
        _state.update {
            it.copy(snackbarMsg = consumed())
        }

    companion object {
        private const val INVALID_CHAT_HANDLE = -1L
        private const val INVALID_POSITION = -1
        private const val DEFAULT_RING_TIMEOUT_SECONDS = 40L
        private const val DEFAULT_GROUP_SNACKBARS_TIMEOUT_MILLISECONDS = 500L
    }
}
