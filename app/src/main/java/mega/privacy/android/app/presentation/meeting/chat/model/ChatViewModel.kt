package mega.privacy.android.app.presentation.meeting.chat.model

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.extensions.isPast
import mega.privacy.android.app.presentation.meeting.chat.mapper.InviteParticipantResultMapper
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatConnectionStatus
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.statistics.EndCallForAll
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.account.MonitorStorageStateEventUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.EndCallUseCase
import mega.privacy.android.domain.usecase.chat.GetCustomSubtitleListUseCase
import mega.privacy.android.domain.usecase.chat.InviteToChatUseCase
import mega.privacy.android.domain.usecase.chat.IsChatNotificationMuteUseCase
import mega.privacy.android.domain.usecase.chat.MonitorACallInThisChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatConnectionStateUseCase
import mega.privacy.android.domain.usecase.chat.MonitorParticipatingInACallUseCase
import mega.privacy.android.domain.usecase.chat.MonitorUserChatStatusByHandleUseCase
import mega.privacy.android.domain.usecase.chat.UnmuteChatNotificationUseCase
import mega.privacy.android.domain.usecase.contact.GetMyUserHandleUseCase
import mega.privacy.android.domain.usecase.contact.GetParticipantFirstNameUseCase
import mega.privacy.android.domain.usecase.contact.GetUserOnlineStatusByHandleUseCase
import mega.privacy.android.domain.usecase.contact.MonitorAllContactParticipantsInChatUseCase
import mega.privacy.android.domain.usecase.contact.MonitorHasAnyContactUseCase
import mega.privacy.android.domain.usecase.contact.MonitorUserLastGreenUpdatesUseCase
import mega.privacy.android.domain.usecase.contact.RequestUserLastGreenUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.meeting.IsChatStatusConnectedForCallUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import timber.log.Timber
import java.util.Collections
import java.util.concurrent.TimeUnit
import javax.inject.Inject

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
@HiltViewModel
internal class ChatViewModel @Inject constructor(
    private val isChatNotificationMuteUseCase: IsChatNotificationMuteUseCase,
    private val getChatRoomUseCase: GetChatRoom,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val getUserOnlineStatusByHandleUseCase: GetUserOnlineStatusByHandleUseCase,
    private val monitorUserChatStatusByHandleUseCase: MonitorUserChatStatusByHandleUseCase,
    private val monitorParticipatingInACallUseCase: MonitorParticipatingInACallUseCase,
    private val monitorACallInThisChatUseCase: MonitorACallInThisChatUseCase,
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
    private val endCallUseCase: EndCallUseCase,
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(ChatUiState())
    val state = _state.asStateFlow()

    private val chatId = savedStateHandle.get<Long>(Constants.CHAT_ID)
    private val usersTyping = Collections.synchronizedMap(mutableMapOf<Long, String?>())
    private val jobs = mutableMapOf<Long, Job>()

    private val ChatRoom.isPrivateRoom: Boolean
        get() = !isGroup || !isPublic

    private val ChatRoom.haveAtLeastReadPermission: Boolean
        get() = ownPrivilege != ChatRoomPermission.Unknown
                && ownPrivilege != ChatRoomPermission.Removed

    private var monitorAllContactParticipantsInChatJob: Job? = null

    init {
        monitorParticipatingInACall()
        monitorStorageStateEvent()
        chatId?.let {
            updateChatId(it)
            getChatRoom(it)
            getNotificationMute(it)
            getChatConnectionState(it)
            getScheduledMeeting(it)
            monitorACallInThisChat(it)
            monitorChatRoom(it)
            monitorNotificationMute(it)
            monitorChatConnectionState(it)
            monitorNetworkConnectivity(it)
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

    private fun getScheduledMeeting(chatId: Long) {
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

    private fun monitorNetworkConnectivity(chatId: Long) {
        viewModelScope.launch {
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

    private fun updateChatId(chatId: Long) {
        viewModelScope.launch {
            _state.update { it.copy(chatId = chatId) }
        }
    }

    private fun monitorChatConnectionState(chatId: Long) {
        viewModelScope.launch {
            monitorChatConnectionStateUseCase()
                .filter { it.chatId == chatId }
                .collect { state ->
                    if (state.chatConnectionStatus != ChatConnectionStatus.Online) {
                        _state.update {
                            it.copy(isConnected = false)
                        }
                    } else {
                        getChatConnectionState(chatId = chatId)
                    }
                }
        }
    }

    private fun getChatConnectionState(chatId: Long) {
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

    private fun getChatRoom(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                getChatRoomUseCase(chatId)
            }.onSuccess { chatRoom ->
                chatRoom?.let {
                    with(chatRoom) {
                        checkCustomTitle()
                        _state.update { state ->
                            state.copy(
                                title = title,
                                isPrivateChat = chatRoom.isPrivateRoom,
                                myPermission = ownPrivilege,
                                isPreviewMode = isPreview,
                                isGroup = isGroup,
                                isOpenInvite = isOpenInvite,
                                isActive = isActive,
                                isArchived = isArchived,
                                isMeeting = isMeeting,
                                participantsCount = getNumberParticipants(),
                                isWaitingRoom = isWaitingRoom
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
        if (isGroup && hasCustomTitle) {
            viewModelScope.launch {
                runCatching { getCustomSubtitleListUseCase(chatId, peerHandlesList, isPreview) }
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

    private fun getNotificationMute(chatId: Long) {
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

    private fun monitorChatRoom(chatId: Long) {
        viewModelScope.launch {
            monitorChatRoomUpdates(chatId)
                .collect { chat ->
                    with(chat) {
                        changes?.forEach { change ->
                            when (change) {
                                ChatRoomChange.Title -> {
                                    checkCustomTitle()
                                    _state.update { state -> state.copy(title = title) }
                                }

                                ChatRoomChange.ChatMode -> _state.update { state ->
                                    state.copy(isPrivateChat = !isPublic)
                                }

                                ChatRoomChange.OwnPrivilege -> _state.update { state ->
                                    state.copy(
                                        myPermission = ownPrivilege,
                                        isActive = isActive,
                                        participantsCount = getNumberParticipants()
                                    )
                                }

                                ChatRoomChange.OpenInvite -> _state.update { state ->
                                    state.copy(isOpenInvite = isOpenInvite)
                                }

                                ChatRoomChange.Closed -> _state.update { state ->
                                    state.copy(
                                        myPermission = ownPrivilege,
                                        isActive = isActive,
                                        participantsCount = getNumberParticipants()
                                    )
                                }

                                ChatRoomChange.Archive -> _state.update { state ->
                                    state.copy(isArchived = isArchived)
                                }

                                ChatRoomChange.UserTyping -> {
                                    if (userTyping != getMyUserHandleUseCase()) {
                                        handleUserTyping(userTyping)
                                    }
                                }

                                ChatRoomChange.UserStopTyping -> handleUserStopTyping(userTyping)

                                ChatRoomChange.Participants -> {
                                    checkCustomTitle()
                                    _state.update { state ->
                                        state.copy(participantsCount = getNumberParticipants())
                                    }
                                    monitorAllContactParticipantsInChat(peerHandlesList)
                                }

                                ChatRoomChange.WaitingRoom -> {
                                    _state.update { state ->
                                        state.copy(isWaitingRoom = isWaitingRoom)
                                    }
                                }

                                else -> {}
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

    private fun monitorNotificationMute(chatId: Long) {
        viewModelScope.launch {
            monitorUpdatePushNotificationSettingsUseCase().collect { changed ->
                if (changed) {
                    getNotificationMute(chatId)
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
        viewModelScope.launch {
            monitorParticipatingInACallUseCase()
                .catch { Timber.e(it) }
                .collect {
                    Timber.d("Monitor call in progress returned chat id: $it")
                    _state.update { state -> state.copy(currentCall = it) }
                }
        }
    }

    private fun monitorACallInThisChat(chatId: Long) {
        viewModelScope.launch {
            monitorACallInThisChatUseCase(chatId)
                .catch { Timber.e(it) }
                .collect {
                    _state.update { state -> state.copy(hasACallInThisChat = it) }
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
        viewModelScope.launch {
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
        chatId?.let { chatId ->
            viewModelScope.launch {
                runCatching {
                    unmuteChatNotificationUseCase(chatId)
                    _state.update {
                        it.copy(
                            pushNotificationMuteOptionEvent = triggered(
                                ChatPushNotificationMuteOption.Unmute
                            )
                        )
                    }
                }.onFailure { Timber.e(it) }
            }
        }
    }

    fun inviteContactsToChat(chatId: Long, contactsData: List<String>) =
        viewModelScope.launch {
            runCatching {
                inviteToChatUseCase(chatId, contactsData)
            }.onSuccess { result ->
                _state.update { state ->
                    state.copy(
                        inviteToChatResultEvent = triggered(inviteParticipantResultMapper(result))
                    )
                }
            }.onFailure {
                Timber.e(it)
            }
        }

    fun onInviteContactsResultConsumed() {
        _state.update { state -> state.copy(inviteToChatResultEvent = consumed()) }
    }

    fun onPushNotificationMuteOptionEventConsumed() {
        _state.update { state -> state.copy(pushNotificationMuteOptionEvent = consumed()) }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            chatId?.let {
                runCatching { clearChatHistoryUseCase(it) }
                    .onSuccess {
                        _state.update { state -> state.copy(infoToShowEvent = triggered(R.string.clear_history_success)) }
                    }
                    .onFailure {
                        Timber.e("Error clearing chat history $it")
                        _state.update { state -> state.copy(infoToShowEvent = triggered(R.string.clear_history_error)) }
                    }
            }
        }
    }

    fun onInfoToShowEventConsumed() {
        _state.update { state -> state.copy(infoToShowEvent = consumed()) }
    }

    fun endCall() {
        chatId?.let {
            viewModelScope.launch {
                runCatching {
                    endCallUseCase(chatId)
                    sendStatisticsMeetingsUseCase(EndCallForAll())
                }.onFailure {
                    Timber.e(it)
                }
            }
        }
    }

    companion object {
        private const val INVALID_HANDLE = -1L
    }
}
