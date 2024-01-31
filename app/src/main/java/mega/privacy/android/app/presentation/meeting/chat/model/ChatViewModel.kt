package mega.privacy.android.app.presentation.meeting.chat.model

import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.main.megachat.MapsActivity
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.extensions.isPast
import mega.privacy.android.app.presentation.meeting.chat.extension.isJoined
import mega.privacy.android.app.presentation.meeting.chat.mapper.InviteParticipantResultMapper
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.statistics.EndCallForAll
import mega.privacy.android.domain.entity.transfer.MultiTransferEvent
import mega.privacy.android.domain.exception.chat.ResourceDoesNotExistChatException
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.MonitorContactCacheUpdates
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.CloseChatPreviewUseCase
import mega.privacy.android.domain.usecase.chat.EnableGeolocationUseCase
import mega.privacy.android.domain.usecase.chat.EndCallUseCase
import mega.privacy.android.domain.usecase.chat.GetChatMuteOptionListUseCase
import mega.privacy.android.domain.usecase.chat.GetCustomSubtitleListUseCase
import mega.privacy.android.domain.usecase.chat.HoldChatCallUseCase
import mega.privacy.android.domain.usecase.chat.InviteToChatUseCase
import mega.privacy.android.domain.usecase.chat.IsAnonymousModeUseCase
import mega.privacy.android.domain.usecase.chat.IsChatNotificationMuteUseCase
import mega.privacy.android.domain.usecase.chat.IsGeolocationEnabledUseCase
import mega.privacy.android.domain.usecase.chat.MonitorCallInChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatConnectionStateUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatPendingChangesUseCase
import mega.privacy.android.domain.usecase.chat.MonitorLeavingChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorParticipatingInACallInOtherChatsUseCase
import mega.privacy.android.domain.usecase.chat.MonitorUserChatStatusByHandleUseCase
import mega.privacy.android.domain.usecase.chat.MuteChatNotificationForChatRoomsUseCase
import mega.privacy.android.domain.usecase.chat.OpenChatLinkUseCase
import mega.privacy.android.domain.usecase.chat.UnmuteChatNotificationUseCase
import mega.privacy.android.domain.usecase.chat.link.JoinPublicChatUseCase
import mega.privacy.android.domain.usecase.chat.link.MonitorJoiningChatUseCase
import mega.privacy.android.domain.usecase.chat.message.SendChatAttachmentsUseCase
import mega.privacy.android.domain.usecase.chat.message.SendLocationMessageUseCase
import mega.privacy.android.domain.usecase.chat.message.SendTextMessageUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetParticipantFirstNameUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.contact.MonitorAllContactParticipantsInChatUseCase
import mega.privacy.android.domain.usecase.contact.MonitorHasAnyContactUseCase
import mega.privacy.android.domain.usecase.contact.MonitorUserLastGreenUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import mega.privacy.android.domain.usecase.file.CreateNewImageUriUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.meeting.HangChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.IsChatStatusConnectedForCallUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.meeting.StartCallUseCase
import mega.privacy.android.domain.usecase.meeting.StartChatCallNoRingingUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * Extra Action
 */
const val EXTRA_ACTION = "ACTION"

/**
 * Extra Link
 */
const val EXTRA_LINK = "LINK"

/**
 * Chat view model.
 *
 * @property isChatNotificationMuteUseCase
 * @property getChatRoomUseCase
 * @property monitorChatRoomUpdates
 * @property monitorUpdatePushNotificationSettingsUseCase
 * @property monitorUserChatStatusByHandleUseCase
 * @property state UI state.
 *
 * @param savedStateHandle
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val isChatNotificationMuteUseCase: IsChatNotificationMuteUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase,
    private val monitorUserChatStatusByHandleUseCase: MonitorUserChatStatusByHandleUseCase,
    private val monitorParticipatingInACallInOtherChatsUseCase: MonitorParticipatingInACallInOtherChatsUseCase,
    private val monitorCallInChatUseCase: MonitorCallInChatUseCase,
    private val monitorStorageStateEventUseCase: MonitorStorageStateEventUseCase,
    private val monitorChatConnectionStateUseCase: MonitorChatConnectionStateUseCase,
    private val isChatStatusConnectedForCallUseCase: IsChatStatusConnectedForCallUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val requestUserLastGreenUseCase: RequestUserLastGreenUseCase,
    private val monitorUserLastGreenUpdatesUseCase: MonitorUserLastGreenUpdatesUseCase,
    private val getParticipantFirstNameUseCase: GetParticipantFirstNameUseCase,
    private val getMyUserHandleUseCase: GetMyUserHandleUseCase,
    private val getScheduledMeetingByChatUseCase: GetScheduledMeetingByChat,
    private val monitorHasAnyContactUseCase: MonitorHasAnyContactUseCase,
    private val passcodeManagement: PasscodeManagement,
    private val getCustomSubtitleListUseCase: GetCustomSubtitleListUseCase,
    private val monitorAllContactParticipantsInChatUseCase: MonitorAllContactParticipantsInChatUseCase,
    private val inviteToChatUseCase: InviteToChatUseCase,
    private val inviteParticipantResultMapper: InviteParticipantResultMapper,
    private val unmuteChatNotificationUseCase: UnmuteChatNotificationUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val archiveChatUseCase: ArchiveChatUseCase,
    private val endCallUseCase: EndCallUseCase,
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase,
    private val startCallUseCase: StartCallUseCase,
    private val chatManagement: ChatManagement,
    private val getChatMuteOptionListUseCase: GetChatMuteOptionListUseCase,
    private val muteChatNotificationForChatRoomsUseCase: MuteChatNotificationForChatRoomsUseCase,
    private val startChatCallNoRingingUseCase: StartChatCallNoRingingUseCase,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val startMeetingInWaitingRoomChatUseCase: StartMeetingInWaitingRoomChatUseCase,
    private val isGeolocationEnabledUseCase: IsGeolocationEnabledUseCase,
    private val enableGeolocationUseCase: EnableGeolocationUseCase,
    private val sendTextMessageUseCase: SendTextMessageUseCase,
    private val holdChatCallUseCase: HoldChatCallUseCase,
    private val hangChatCallUseCase: HangChatCallUseCase,
    private val monitorContactCacheUpdates: MonitorContactCacheUpdates,
    private val joinPublicChatUseCase: JoinPublicChatUseCase,
    private val isAnonymousModeUseCase: IsAnonymousModeUseCase,
    private val savedStateHandle: SavedStateHandle,
    private val openChatLinkUseCase: OpenChatLinkUseCase,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val closeChatPreviewUseCase: CloseChatPreviewUseCase,
    private val createNewImageUriUseCase: CreateNewImageUriUseCase,
    private val monitorJoiningChatUseCase: MonitorJoiningChatUseCase,
    private val monitorLeavingChatUseCase: MonitorLeavingChatUseCase,
    private val sendLocationMessageUseCase: SendLocationMessageUseCase,
    private val sendChatAttachmentsUseCase: SendChatAttachmentsUseCase,
    private val monitorChatPendingChangesUseCase: MonitorChatPendingChangesUseCase,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    private val chatId: Long = savedStateHandle[Constants.CHAT_ID]
        ?: throw IllegalStateException("Chat screen must have a chat room id")
    private val chatLink: String
        get() = savedStateHandle.get<String>(EXTRA_LINK).orEmpty()

    private val usersTyping = Collections.synchronizedMap(mutableMapOf<Long, String?>())
    private val jobs = mutableMapOf<Long, Job>()

    private val ChatRoom.haveAtLeastReadPermission: Boolean
        get() = ownPrivilege != ChatRoomPermission.Unknown
                && ownPrivilege != ChatRoomPermission.Removed

    private var monitorAllContactParticipantsInChatJob: Job? = null
    private var monitorChatRoomUpdatesJob: Job? = null
    private var monitorHasAnyContactJob: Job? = null
    private var monitorCallInChatJob: Job? = null
    private var monitorParticipatingInACallInOtherChatsJob: Job? = null
    private var monitorChatConnectionStateJob: Job? = null
    private var monitorConnectivityJob: Job? = null

    init {
        checkGeolocation()
        monitorStorageStateEvent()
        loadChatOrPreview()
        checkAnonymousMode()
        monitorContactCacheUpdate()
        monitorNotificationMute()
        monitorJoiningChat()
        monitorLeavingChat()
        monitorChatRoomPreference()
    }

    private fun monitorChatRoomPreference() {
        viewModelScope.launch {
            monitorChatPendingChangesUseCase(chatId)
                .catch { Timber.e(it) }
                .collect { preference ->
                    _state.update { state -> state.copy(sendingText = preference.draftMessage) }
                }
        }
    }

    private fun loadChatOrPreview() {
        val action = savedStateHandle.get<String>(EXTRA_ACTION).orEmpty()
        if (chatLink.isNotEmpty()) {
            val requireJoin = action == Constants.ACTION_JOIN_OPEN_CHAT_LINK
            viewModelScope.launch {
                runCatching {
                    openChatLinkUseCase(
                        chatLink = chatLink,
                        chatId = chatId,
                        requireJoin = requireJoin,
                    )
                }.onSuccess {
                    loadChatRoom()
                }.onFailure {
                    Timber.e(it)
                    val infoToShow = if (it is ResourceDoesNotExistChatException) {
                        InfoToShow.SimpleString(stringId = R.string.invalid_chat_link)
                    } else {
                        InfoToShow.SimpleString(stringId = R.string.error_general_nodes)
                    }
                    _state.update { state -> state.copy(infoToShowEvent = triggered(infoToShow)) }
                }
            }
        } else {
            loadChatRoom()
        }
    }

    private fun checkAnonymousMode() {
        viewModelScope.launch {
            val isAnonymousMode = isAnonymousModeUseCase()
            _state.update { state -> state.copy(isAnonymousMode = isAnonymousMode) }
        }
    }

    private fun monitorContactCacheUpdate() {
        viewModelScope.launch {
            monitorContactCacheUpdates()
                // I don't know why sdk emit 2 the same events, add debounce to optimize
                .debounce(300L.toDuration(DurationUnit.MILLISECONDS))
                .catch { Timber.e(it) }
                .collect {
                    Timber.d("Contact cache update: $it")
                    _state.update { state -> state.copy(userUpdate = it) }
                }
        }
    }

    private fun loadChatRoom() {
        getChatRoom()
        getNotificationMute()
        getChatConnectionState()
        getScheduledMeeting()
        monitorACallInThisChat()
        monitorParticipatingInACall()
        monitorChatRoom()
        monitorChatConnectionState()
        monitorNetworkConnectivity()
    }

    private fun monitorJoiningChat() {
        viewModelScope.launch {
            monitorJoiningChatUseCase(chatId)
                .collectLatest { isJoining ->
                    if (!isJoining) {
                        loadChatRoom()
                    } else {
                        _state.update { state -> state.copy(isJoining = true) }
                    }
                }
        }
    }

    private fun monitorLeavingChat() {
        viewModelScope.launch {
            monitorLeavingChatUseCase(chatId)
                .collectLatest { _state.update { state -> state.copy(isLeaving = it) } }
        }
    }

    private fun monitorAllContactParticipantsInChat(peerHandles: List<Long>) {
        monitorAllContactParticipantsInChatJob?.cancel()
        monitorAllContactParticipantsInChatJob = viewModelScope.launch {
            monitorAllContactParticipantsInChatUseCase(peerHandles)
                .catch { Timber.e(it) }
                .collect { allContactsParticipateInChat ->
                    _state.update { state -> state.copy(allContactsParticipateInChat = allContactsParticipateInChat) }
                }
        }
    }

    private fun getScheduledMeeting() {
        viewModelScope.launch {
            runCatching {
                getScheduledMeetingByChatUseCase(chatId)
            }.onSuccess { scheduledMeetingList ->
                scheduledMeetingList?.firstOrNull { it.parentSchedId == INVALID_HANDLE }
                    ?.let { meeting ->
                        _state.update {
                            it.copy(
                                schedIsPending = !meeting.isPast(),
                                scheduledMeeting = meeting
                            )
                        }
                    }
            }.onFailure {
                Timber.e(it)
                _state.update { state -> state.copy(scheduledMeeting = null) }
            }
        }
    }

    private fun monitorNetworkConnectivity() {
        monitorConnectivityJob?.cancel()
        monitorConnectivityJob = viewModelScope.launch {
            monitorConnectivityUseCase()
                .collect { networkConnected ->
                    val isChatConnected = if (networkConnected) {
                        isChatStatusConnectedForCallUseCase(chatId = chatId)
                    } else {
                        false
                    }

                    _state.update {
                        it.copy(isConnected = isChatConnected)
                    }
                }
        }
    }

    private fun monitorChatConnectionState() {
        monitorChatConnectionStateJob?.cancel()
        monitorChatConnectionStateJob = viewModelScope.launch {
            monitorChatConnectionStateUseCase()
                .filter { it.chatId == chatId }
                .collect { state ->
                    if (state.chatConnectionStatus != ChatConnectionStatus.Online) {
                        _state.update {
                            it.copy(isConnected = false)
                        }
                    } else {
                        getChatConnectionState()
                    }
                }
        }
    }

    private fun getChatConnectionState() {
        viewModelScope.launch {
            runCatching {
                isChatStatusConnectedForCallUseCase(chatId = chatId)
            }.onSuccess { connected ->
                _state.update { state ->
                    state.copy(isConnected = connected)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun monitorStorageStateEvent() {
        viewModelScope.launch {
            monitorStorageStateEventUseCase()
                .collect { storageState ->
                    _state.update { state -> state.copy(storageState = storageState.storageState) }
                }
        }
    }

    private fun getChatRoom() {
        viewModelScope.launch {
            runCatching {
                getChatRoomUseCase(chatId)
            }.onSuccess { chatRoom ->
                chatRoom?.let {
                    with(chatRoom) {
                        checkCustomTitle()
                        val participantsCount = getNumberParticipants()
                        _state.update { state ->
                            val isJoining = false.takeIf { !isPreview } ?: state.isJoining
                            state.copy(
                                chat = chatRoom,
                                isJoining = isJoining,
                                participantsCount = participantsCount,
                            )
                        }
                        if (peerHandlesList.isNotEmpty()) {
                            if (!isGroup) {
                                peerHandlesList[0].let {
                                    getUserChatStatus(it)
                                    monitorUserOnlineStatusUpdates(it)
                                    monitorUserLastGreen(it)
                                }
                            } else {
                                monitorAllContactParticipantsInChat(peerHandlesList)
                                monitorHasAnyContact()
                            }
                        }
                    }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun ChatRoom.checkCustomTitle() {
        if (!isPreview && isGroup && hasCustomTitle) {
            viewModelScope.launch {
                runCatching { getCustomSubtitleListUseCase(chatId, peerHandlesList) }
                    .onSuccess { customSubtitleList ->
                        _state.update { state -> state.copy(customSubtitleList = customSubtitleList) }
                    }
                    .onFailure { Timber.w(it) }
            }
        }
    }

    private fun ChatRoom.getNumberParticipants() =
        (peerCount + if (haveAtLeastReadPermission) 1 else 0)
            .takeIf { isGroup }

    private fun getUserChatStatus(userHandle: Long) {
        viewModelScope.launch {
            runCatching {
                getUserOnlineStatusByHandleUseCase(userHandle)
            }.onSuccess { userChatStatus ->
                updateUserChatStatus(userHandle, userChatStatus)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun getNotificationMute() {
        viewModelScope.launch {
            runCatching {
                isChatNotificationMuteUseCase(chatId)
            }.onSuccess { isMute ->
                _state.update { it.copy(isChatNotificationMute = isMute) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun monitorChatRoom() {
        monitorChatRoomUpdatesJob?.cancel()
        monitorChatRoomUpdatesJob = viewModelScope.launch {
            monitorChatRoomUpdates(chatId)
                .collect { chat ->
                    with(chat) {
                        _state.update { state -> state.copy(chat = chat) }

                        changes?.forEach { change ->
                            when (change) {
                                ChatRoomChange.Title -> {
                                    checkCustomTitle()
                                }

                                ChatRoomChange.OwnPrivilege,
                                ChatRoomChange.Closed,
                                -> {
                                    _state.update { state ->
                                        state.copy(
                                            participantsCount = getNumberParticipants()
                                        )
                                    }
                                }

                                ChatRoomChange.UserTyping -> {
                                    if (userTyping != getMyUserHandleUseCase()) {
                                        handleUserTyping(userTyping)
                                    }
                                }

                                ChatRoomChange.UserStopTyping -> {
                                    handleUserStopTyping(userTyping)
                                }

                                ChatRoomChange.Participants -> {
                                    checkCustomTitle()
                                    _state.update { state ->
                                        state.copy(participantsCount = getNumberParticipants())
                                    }
                                    monitorAllContactParticipantsInChat(peerHandlesList)
                                }

                                else -> {
                                }
                            }
                        }
                    }
                }
        }
    }

    private fun handleUserStopTyping(userTypingHandle: Long) {
        jobs[userTypingHandle]?.cancel()
        usersTyping.remove(userTypingHandle)
        _state.update { state ->
            state.copy(usersTyping = usersTyping.values.toList())
        }
    }

    private fun handleUserTyping(userTypingHandle: Long) {
        // if user is in the map, we don't need to add again
        if (!usersTyping.contains(userTypingHandle)) {
            viewModelScope.launch {
                val firstName = getParticipantFirstNameUseCase(userTypingHandle)
                usersTyping[userTypingHandle] = firstName
                _state.update { state ->
                    state.copy(usersTyping = usersTyping.values.toList())
                }
            }
        }
        // if user continue typing, cancel timer and start new timer
        jobs[userTypingHandle]?.cancel()
        jobs[userTypingHandle] = viewModelScope.launch {
            delay(TimeUnit.SECONDS.toMillis(5))
            usersTyping.remove(userTypingHandle)
            _state.update { state ->
                state.copy(usersTyping = usersTyping.values.toList())
            }
        }
    }

    private fun monitorNotificationMute() {
        viewModelScope.launch {
            monitorUpdatePushNotificationSettingsUseCase().collect { changed ->
                if (changed) {
                    getNotificationMute()
                }
            }
        }
    }

    private fun monitorUserOnlineStatusUpdates(userHandle: Long) {
        viewModelScope.launch {
            monitorUserChatStatusByHandleUseCase(userHandle).conflate()
                .collect { userChatStatus ->
                    updateUserChatStatus(userHandle, userChatStatus)
                }
        }
    }

    private fun updateUserChatStatus(userHandle: Long, userChatStatus: UserChatStatus) {
        viewModelScope.launch {
            if (userChatStatus != UserChatStatus.Online) {
                _state.update { state -> state.copy(userChatStatus = userChatStatus) }
                runCatching { requestUserLastGreenUseCase(userHandle) }
                    .onFailure { Timber.e(it) }
            } else {
                _state.update { state ->
                    state.copy(
                        userChatStatus = userChatStatus,
                        userLastGreen = null
                    )
                }
            }
        }
    }

    private fun monitorParticipatingInACall() {
        monitorParticipatingInACallInOtherChatsJob?.cancel()
        monitorParticipatingInACallInOtherChatsJob = viewModelScope.launch {
            monitorParticipatingInACallInOtherChatsUseCase(chatId)
                .catch { Timber.e(it) }
                .collect {
                    Timber.d("Monitor call in progress returned chat id: $it")
                    _state.update { state -> state.copy(callsInOtherChats = it) }
                }
        }
    }

    private fun monitorACallInThisChat() {
        monitorCallInChatJob?.cancel()
        monitorCallInChatJob = viewModelScope.launch {
            monitorCallInChatUseCase(chatId)
                .catch { Timber.e(it) }
                .collect {
                    _state.update { state -> state.copy(callInThisChat = it) }
                }
        }
    }

    private fun monitorUserLastGreen(userHandle: Long) {
        viewModelScope.launch {
            monitorUserLastGreenUpdatesUseCase(userHandle).conflate()
                .collect { userLastGreen ->
                    if (state.value.userChatStatus != UserChatStatus.Online) {
                        _state.update { state -> state.copy(userLastGreen = userLastGreen) }
                    }
                }
        }
    }

    private fun monitorHasAnyContact() {
        monitorHasAnyContactJob?.cancel()
        monitorHasAnyContactJob = viewModelScope.launch {
            monitorHasAnyContactUseCase().conflate()
                .collect { hasAnyContact ->
                    _state.update { state -> state.copy(hasAnyContact = hasAnyContact) }
                }
        }
    }

    /**
     * Get another call participating
     *
     */
    fun enablePasscodeCheck() {
        passcodeManagement.showPasscodeScreen = true
    }


    /**
     * Handle action press
     *
     * @param action [ChatRoomMenuAction].
     */
    fun handleActionPress(action: ChatRoomMenuAction) {
        when (action) {
            is ChatRoomMenuAction.Unmute -> unmutePushNotification()
            else -> {}
        }
    }

    private fun unmutePushNotification() {
        viewModelScope.launch {
            runCatching {
                unmuteChatNotificationUseCase(chatId)
                _state.update {
                    it.copy(
                        infoToShowEvent = triggered(
                            InfoToShow.MuteOptionResult(result = ChatPushNotificationMuteOption.Unmute)
                        )
                    )
                }
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Show the dialog of selecting chat mute options
     */
    fun showMutePushNotificationDialog() {
        viewModelScope.launch {
            val muteOptionList = getChatMuteOptionListUseCase(listOf(chatId))
            _state.update {
                it.copy(mutePushNotificationDialogEvent = triggered(muteOptionList))
            }
        }
    }

    /**
     * Consume the event of showing the dialog of selecting chat mute options.
     */
    fun onShowMutePushNotificationDialogConsumed() {
        _state.update { state -> state.copy(mutePushNotificationDialogEvent = consumed()) }
    }

    /**
     * Mute chat push notification based on user selection. And once mute operation succeeds,
     * send [InfoToShow] to show a message to indicate the result in UI.
     *
     * @param option [ChatPushNotificationMuteOption]
     */
    fun mutePushNotification(option: ChatPushNotificationMuteOption) {
        viewModelScope.launch {
            runCatching {
                muteChatNotificationForChatRoomsUseCase(listOf(chatId), option)
                _state.update {
                    it.copy(
                        infoToShowEvent = triggered(
                            InfoToShow.MuteOptionResult(result = option)
                        )
                    )
                }
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Invite contacts to chat.
     */
    fun inviteContactsToChat(chatId: Long, contactsData: List<String>) =
        viewModelScope.launch {
            runCatching {
                inviteToChatUseCase(chatId, contactsData)
            }.onSuccess { result ->
                _state.update { state ->
                    state.copy(
                        infoToShowEvent = triggered(
                            InfoToShow.InviteContactResult(
                                result = inviteParticipantResultMapper(result)
                            )
                        )
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }

    /**
     * Clear chat history.
     */
    fun clearChatHistory() {
        viewModelScope.launch {
            runCatching { clearChatHistoryUseCase(chatId) }
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            infoToShowEvent = triggered(InfoToShow.SimpleString(stringId = R.string.clear_history_success))
                        )
                    }
                }
                .onFailure {
                    Timber.e("Error clearing chat history $it")
                    _state.update { state ->
                        state.copy(
                            infoToShowEvent = triggered(InfoToShow.SimpleString(stringId = R.string.clear_history_error))
                        )
                    }
                }
        }
    }

    /**
     * Consumes the event of showing info.
     */
    fun onInfoToShowEventConsumed() {
        _state.update { state -> state.copy(infoToShowEvent = consumed()) }
    }

    /**
     * Ends a call.
     */
    fun endCall() {
        viewModelScope.launch {
            runCatching {
                endCallUseCase(chatId)
                sendStatisticsMeetingsUseCase(EndCallForAll())
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Archive chat.
     */
    fun archiveChat() {
        viewModelScope.launch {
            runCatching { archiveChatUseCase(chatId, true) }
                .onFailure {
                    Timber.e("Error archiving chat $it")
                    _state.update { state ->
                        state.copy(
                            infoToShowEvent = triggered(
                                InfoToShow.StringWithParams(
                                    stringId = R.string.error_archive_chat,
                                    args = state.title?.let { title -> listOf(title) }.orEmpty()
                                )
                            )
                        )
                    }
                }
        }
    }

    /**
     * Unarchive chat.
     */
    fun unarchiveChat() {
        viewModelScope.launch {
            runCatching { archiveChatUseCase(chatId, false) }
                .onFailure {
                    Timber.e("Error unarchiving chat $it")
                    _state.update { state ->
                        state.copy(
                            infoToShowEvent = triggered(
                                InfoToShow.StringWithParams(
                                    stringId = R.string.error_unarchive_chat,
                                    args = state.title?.let { title -> listOf(title) }.orEmpty()
                                )
                            )
                        )
                    }
                }
        }
    }

    /**
     * Start a call.
     */
    fun startCall(video: Boolean) {
        viewModelScope.launch {
            runCatching { startCallUseCase(chatId, video) }
                .onSuccess { call ->
                    setCallReady(call)
                }.onFailure { Timber.e("Exception starting call $it") }
        }
    }

    private fun setCallReady(call: ChatCall?) {
        call?.let {
            chatManagement.setSpeakerStatus(call.chatId, call.hasLocalVideo)
            chatManagement.setRequestSentCall(call.callId, call.isOutgoing)
            passcodeManagement.showPasscodeScreen = true
            _state.update { state ->
                state.copy(callInThisChat = call, isStartingCall = true)
            }
        }
    }

    /**
     * On call started.
     */
    fun onCallStarted() {
        _state.update { state -> state.copy(isStartingCall = false) }
    }

    /**
     * On opened waiting room
     */
    fun onWaitingRoomOpened() {
        _state.update { state -> state.copy(openWaitingRoomScreen = false) }
    }

    /**
     * Start or join a meeting.
     */
    fun onStartOrJoinMeeting(isStarted: Boolean) {
        val isWaitingRoom = state.value.isWaitingRoom
        if (isStarted) {
            val isHost = state.value.myPermission == ChatRoomPermission.Moderator
            if (isWaitingRoom && !isHost) {
                _state.update { state -> state.copy(openWaitingRoomScreen = true) }
            } else {
                onAnswerCall()
            }
        } else {
            if (isWaitingRoom) {
                startWaitingRoomMeeting()
            } else {
                startMeeting()
            }
        }
    }

    private fun startWaitingRoomMeeting() {
        val isHost = state.value.myPermission == ChatRoomPermission.Moderator
        if (isHost) {
            viewModelScope.launch {
                runCatching {
                    val schedId = state.value.scheduledMeeting?.schedId ?: -1L
                    startMeetingInWaitingRoomChatUseCase(
                        chatId = chatId,
                        schedIdWr = schedId,
                        enabledVideo = false,
                        enabledAudio = true,
                    )
                }.onSuccess { chatCall ->
                    setCallReady(chatCall)
                }.onFailure {
                    Timber.e(it)
                }
            }
        } else {
            _state.update { state -> state.copy(openWaitingRoomScreen = true) }
        }
    }

    private fun startMeeting() {
        viewModelScope.launch {
            runCatching {
                val scheduledMeeting = requireNotNull(state.value.scheduledMeeting)
                startChatCallNoRingingUseCase(
                    chatId = chatId,
                    schedId = scheduledMeeting.schedId,
                    enabledVideo = false,
                    enabledAudio = true
                )
            }.onSuccess { chatCall ->
                setCallReady(chatCall)
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Answers a call.
     */
    fun onAnswerCall() {
        viewModelScope.launch {
            chatManagement.addJoiningCallChatId(chatId)
            runCatching {
                answerChatCallUseCase(chatId = chatId, video = false, audio = true)
            }.onSuccess { call ->
                call?.apply {
                    chatManagement.removeJoiningCallChatId(chatId)
                    rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                    _state.update { state -> state.copy(isStartingCall = true) }
                }
            }.onFailure {
                Timber.w("Exception answering call: $it")
                chatManagement.removeJoiningCallChatId(chatId)
            }
        }
    }

    private fun checkGeolocation() {
        viewModelScope.launch {
            runCatching { isGeolocationEnabledUseCase() }
                .onSuccess { isGeolocationEnabled ->
                    _state.update { state -> state.copy(isGeolocationEnabled = isGeolocationEnabled) }
                }.onFailure {
                    Timber.e(it)
                }
        }
    }

    /**
     * Enables geolocation.
     */
    fun onEnableGeolocation() {
        viewModelScope.launch {
            runCatching { enableGeolocationUseCase() }
                .onSuccess {
                    _state.update { state -> state.copy(isGeolocationEnabled = true) }
                }.onFailure {
                    Timber.e(it)
                }
        }
    }

    /**
     * Sends a text message.
     */
    fun sendTextMessage(message: String) {
        viewModelScope.launch {
            sendTextMessageUseCase(chatId, message)
        }
    }

    /**
     * Holds an existing in progress call and answers a new one.
     */
    fun onHoldAndAnswerCall() {
        viewModelScope.launch {
            val callToHold = state.value.callsInOtherChats
                .find { it.status?.isJoined == true }

            callToHold?.chatId?.let {
                if (callToHold.isOnHold) {
                    // The call is already on hold, just answer
                    onAnswerCall()
                } else {
                    runCatching {
                        holdChatCallUseCase(chatId = it, setOnHold = true)
                    }.onFailure { Timber.e(it) }
                        .onSuccess { onAnswerCall() }
                }
            } ?: run {
                // The call finished before setting on hold, just answer
                onAnswerCall()
            }
        }
    }

    /**
     * Ends an existing in progress call and answers a new one.
     */
    fun onEndAndAnswerCall() {
        viewModelScope.launch {
            state.value.callsInOtherChats
                .find { it.status?.isJoined == true && !it.isOnHold }?.callId?.let {
                    runCatching {
                        hangChatCallUseCase(it)
                    }.onFailure { Timber.e(it) }
                        .onSuccess { onAnswerCall() }
                } ?: run {
                // The call finished before ending, just answer
                onAnswerCall()
            }
        }
    }

    /**
     * On user update handled.
     */
    fun onUserUpdateHandled() {
        _state.update { state -> state.copy(userUpdate = null) }
    }

    /**
     * Sets pending link to join.
     */
    fun onSetPendingJoinLink() {
        chatManagement.pendingJoinLink = savedStateHandle[EXTRA_LINK]
    }

    /**
     * Attaches files.
     */
    fun onAttachFiles(files: List<Uri>) {
        viewModelScope.launch {
            sendChatAttachmentsUseCase(chatId, files.map { it.toString() })
                .catch { Timber.e(it) }
                .collect {
                    if (it is MultiTransferEvent.TransferNotStarted<*>) {
                        Timber.e(it.exception, "${it.item} not attached")
                    }
                }
        }
    }

    /**
     * Joins a public chat.
     */
    fun onJoinChat() {
        viewModelScope.launch {
            runCatching {
                joinPublicChatUseCase(chatId)
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Creates a new image uri.
     */
    suspend fun createNewImageUri(): Uri? {
        Timber.d("createNewImageUri")
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "picture${timeStamp}_.jpg"
        return createNewImageUriUseCase(imageFileName)?.let { Uri.parse(it) }
    }

    /**
     * Send location message.
     */
    fun sendLocationMessage(data: Intent?) {
        data?.let {
            val byteArray = data.getByteArrayExtra(MapsActivity.SNAPSHOT) ?: return
            val latitude = data.getDoubleExtra(MapsActivity.LATITUDE, 0.0).toFloat()
            val longitude = data.getDoubleExtra(MapsActivity.LONGITUDE, 0.0).toFloat()

            viewModelScope.launch {
                val encodedSnapshot = Base64.encodeToString(byteArray, Base64.DEFAULT)
                val tempMessage = sendLocationMessageUseCase(
                    chatId = chatId,
                    latitude = latitude,
                    longitude = longitude,
                    image = encodedSnapshot
                )
            }
        }
    }

    override fun onCleared() {
        if (state.value.isPreviewMode) {
            applicationScope.launch {
                runCatching {
                    closeChatPreviewUseCase(chatId)
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
        super.onCleared()
    }

    companion object {
        private const val INVALID_HANDLE = -1L
    }
}
