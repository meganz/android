package mega.privacy.android.app.presentation.meeting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.MegaApplication.Companion.getPushNotificationSettingManagement
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoUiState
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.meeting.WaitingRoomReminders
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.user.UserChanges
import mega.privacy.android.domain.usecase.GetChatParticipants
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.InviteToChat
import mega.privacy.android.domain.usecase.MonitorUserUpdates
import mega.privacy.android.domain.usecase.RemoveFromChat
import mega.privacy.android.domain.usecase.SetOpenInviteWithChatIdUseCase
import mega.privacy.android.domain.usecase.SetPublicChatToPrivate
import mega.privacy.android.domain.usecase.call.GetChatCallUseCase
import mega.privacy.android.domain.usecase.call.MonitorSFUServerUpgradeUseCase
import mega.privacy.android.domain.usecase.call.OpenOrStartCallUseCase
import mega.privacy.android.domain.usecase.chat.LeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRoomUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.chat.UpdateChatPermissionsUseCase
import mega.privacy.android.domain.usecase.contact.GetMyFullNameUseCase
import mega.privacy.android.domain.usecase.contact.InviteContactWithEmailUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChatUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.SetWaitingRoomRemindersUseCase
import mega.privacy.android.domain.usecase.meeting.SetWaitingRoomUseCase
import mega.privacy.android.domain.usecase.network.IsConnectedToInternetUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ScheduledMeetingInfoActivity view model.
 *
 * @property getChatRoomUseCase                                    [GetChatRoomUseCase]
 * @property getChatParticipants                            [GetChatParticipants]
 * @property getScheduledMeetingByChatUseCase               [GetScheduledMeetingByChatUseCase]
 * @property getVisibleContactsUseCase                      [GetVisibleContactsUseCase]
 * @property inviteToChat                                   [InviteToChat]
 * @property leaveChatUseCase                               [LeaveChatUseCase]
 * @property removeFromChat                                 [RemoveFromChat]
 * @property inviteContactWithEmailUseCase                  [InviteContactWithEmailUseCase]
 * @property setOpenInviteWithChatIdUseCase                 [SetOpenInviteWithChatIdUseCase]
 * @property updateChatPermissionsUseCase                   [UpdateChatPermissionsUseCase]
 * @property getPublicChatToPrivate                         [SetPublicChatToPrivate]
 * @property passcodeManagement                             [PasscodeManagement]
 * @property chatManagement                                 [ChatManagement]
 * @property startConversationUseCase                       [StartConversationUseCase]
 * @property openOrStartCallUseCase                         [OpenOrStartCallUseCase]
 * @property monitorScheduledMeetingUpdatesUseCase          [MonitorScheduledMeetingUpdatesUseCase]
 * @property monitorConnectivityUseCase                     [MonitorConnectivityUseCase]
 * @property monitorChatRoomUpdatesUseCase                  [MonitorChatRoomUpdatesUseCase]
 * @property monitorUpdatePushNotificationSettingsUseCase   [MonitorUpdatePushNotificationSettingsUseCase]
 * @property setChatVideoInDeviceUseCase                    [SetChatVideoInDeviceUseCase]
 * @property deviceGateway                                  [DeviceGateway]
 * @property setWaitingRoomUseCase                          [SetWaitingRoomUseCase]
 * @property setWaitingRoomRemindersUseCase                 [SetWaitingRoomRemindersUseCase]
 * @property getStringFromStringResMapper                   [GetStringFromStringResMapper]
 * @property getMyFullNameUseCase                           [GetMyFullNameUseCase]
 * @property monitorUserUpdates                             [MonitorUserUpdates]
 * @property getChatCallUseCase                             [GetChatCallUseCase]
 * @property monitorChatCallUpdatesUseCase                  [MonitorChatCallUpdatesUseCase]
 * @property uiState                    Current view state as [ScheduledMeetingInfoUiState]
 */
@HiltViewModel
class ScheduledMeetingInfoViewModel @Inject constructor(
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val getChatParticipants: GetChatParticipants,
    private val getScheduledMeetingByChatUseCase: GetScheduledMeetingByChatUseCase,
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase,
    private val inviteToChat: InviteToChat,
    private val leaveChatUseCase: LeaveChatUseCase,
    private val removeFromChat: RemoveFromChat,
    private val inviteContactWithEmailUseCase: InviteContactWithEmailUseCase,
    private val setOpenInviteWithChatIdUseCase: SetOpenInviteWithChatIdUseCase,
    private val updateChatPermissionsUseCase: UpdateChatPermissionsUseCase,
    private val getPublicChatToPrivate: SetPublicChatToPrivate,
    private val passcodeManagement: PasscodeManagement,
    private val chatManagement: ChatManagement,
    private val startConversationUseCase: StartConversationUseCase,
    private val openOrStartCallUseCase: OpenOrStartCallUseCase,
    private val monitorScheduledMeetingUpdatesUseCase: MonitorScheduledMeetingUpdatesUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val monitorChatRoomUpdatesUseCase: MonitorChatRoomUpdatesUseCase,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val deviceGateway: DeviceGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val setWaitingRoomUseCase: SetWaitingRoomUseCase,
    private val setWaitingRoomRemindersUseCase: SetWaitingRoomRemindersUseCase,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
    private val getMyFullNameUseCase: GetMyFullNameUseCase,
    private val monitorUserUpdates: MonitorUserUpdates,
    private val monitorSFUServerUpgradeUseCase: MonitorSFUServerUpgradeUseCase,
    private val getChatCallUseCase: GetChatCallUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScheduledMeetingInfoUiState())
    val uiState = _uiState.asStateFlow()

    val is24HourFormat by lazy { deviceGateway.is24HourFormat() }

    private var scheduledMeetingId: Long = megaChatApiGateway.getChatInvalidHandle()

    private var monitorSFUServerUpgradeJob: Job? = null
    private var monitorChatCallJob: Job? = null

    /**
     * Monitor connectivity event
     */
    val monitorConnectivityEvent = monitorConnectivityUseCase()

    /**
     * Is network connected
     */
    val isConnected: Boolean
        get() = isConnectedToInternetUseCase()

    init {
        getMyFullName()
        monitorMutedChatsUpdates()

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
        monitorSFUServerUpgrade()
    }

    /**
     * Sets chat id and scheduled meeting id
     *
     * @param newChatId                 Chat id.
     * @param newScheduledMeetingId     Scheduled meeting id.
     */
    fun setChatId(newChatId: Long, newScheduledMeetingId: Long) {
        if (newChatId != megaChatApiGateway.getChatInvalidHandle() && newChatId != uiState.value.chatId) {
            _uiState.update {
                it.copy(
                    chatId = newChatId
                )
            }
            scheduledMeetingId = newScheduledMeetingId
            getChat()
            getChatCall()
            getScheduledMeeting()
        }
    }

    /**
     * Get chat room
     */
    private fun getChat() =
        viewModelScope.launch {
            runCatching {
                getChatRoomUseCase(uiState.value.chatId)
            }.onFailure { exception ->
                Timber.e("Chat room does not exist, finish $exception")
                finishActivity()
            }.onSuccess { chat ->
                Timber.d("Chat room exists")
                chat?.apply {
                    if (isActive) {
                        Timber.d("Chat room is active")
                        _uiState.update { state ->
                            state.copy(
                                chatId = chatId,
                                chatTitle = title,
                                isHost = ownPrivilege == ChatRoomPermission.Moderator,
                                isOpenInvite = isOpenInvite || ownPrivilege == ChatRoomPermission.Moderator,
                                enabledAllowNonHostAddParticipantsOption = isOpenInvite,
                                enabledWaitingRoomOption = isWaitingRoom,
                                isPublic = isPublic,
                                myPermission = ownPrivilege,
                            )
                        }

                        loadAllChatParticipants()
                        updateDndSeconds(chatId)
                        updateRetentionTimeSeconds(retentionTime)
                        getChatRoomUpdates(chatId)
                    } else {
                        Timber.d("Chat room is not active, finish")
                        finishActivity()
                    }
                }
            }
        }

    /**
     * Get chat call updates
     */
    private fun getChatCall() {
        viewModelScope.launch {
            runCatching {
                getChatCallUseCase(_uiState.value.chatId)?.let { call ->
                    setShouldShowUserLimitsWarning(call)
                    monitorChatCall(call.callId)
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun monitorChatCall(callId: Long) {
        monitorChatCallJob?.cancel()
        monitorChatCallJob = viewModelScope.launch {
            monitorChatCallUpdatesUseCase()
                .filter { it.callId == callId }
                .catch {
                    Timber.e(it)
                }
                .collect { call ->
                    setShouldShowUserLimitsWarning(call)
                    call.changes?.apply {
                        if (contains(ChatCallChanges.Status)) {
                            Timber.d("Chat call status: ${call.status}")
                            when (call.status) {
                                ChatCallStatus.Destroyed -> {
                                    // Call has ended
                                    _uiState.update { it.copy(shouldShowParticipantsLimitWarning = false) }
                                    monitorChatCallJob?.cancel()
                                }

                                else -> {}
                            }
                        }
                    }
                }
        }
    }

    private fun setShouldShowUserLimitsWarning(call: ChatCall) {
        Timber.d("Call user limit ${call.callUsersLimit} and users in call ${call.peerIdParticipants?.size}")
        if (call.callUsersLimit != -1) {
            val limit = call.callUsersLimit
                ?: MeetingState.FREE_PLAN_PARTICIPANTS_LIMIT
            val shouldShowWarning =
                (call.peerIdParticipants?.size
                    ?: 0) >= limit
            _uiState.update { it.copy(shouldShowParticipantsLimitWarning = shouldShowWarning) }
        } else {
            _uiState.update { it.copy(shouldShowParticipantsLimitWarning = false) }
        }
    }

    /**
     * Monitor muted chats updates
     */
    private fun monitorMutedChatsUpdates() = viewModelScope.launch {
        monitorUpdatePushNotificationSettingsUseCase().collectLatest {
            updateDndSeconds(uiState.value.chatId)
        }
    }

    /**
     * Load all chat participants
     */
    private fun loadAllChatParticipants() = viewModelScope.launch {
        runCatching {
            getChatParticipants(uiState.value.chatId)
                .catch { exception ->
                    Timber.e(exception)
                }
                .collectLatest { list ->
                    Timber.d("Updated list of participants: list ${list.size}")
                    _uiState.update {
                        it.copy(participantItemList = list, numOfParticipants = list.size)
                    }
                    updateFirstAndSecondParticipants()
                }
        }.onFailure { exception ->
            Timber.e(exception)
        }
    }

    /**
     * Update first and last participants
     */
    private fun updateFirstAndSecondParticipants() {
        _uiState.value.participantItemList.let { list ->
            _uiState.update {
                it.copy(
                    firstParticipant = if (list.isNotEmpty()) list.first() else null,
                    secondParticipant = if (list.size > 1) list[1] else null
                )
            }
        }
    }

    /**
     * Get scheduled meeting
     */
    private fun getScheduledMeeting() =
        viewModelScope.launch {
            runCatching {
                getScheduledMeetingByChatUseCase(uiState.value.chatId)
            }.onFailure { exception ->
                Timber.e("Scheduled meeting does not exist, finish $exception")
                finishActivity()
            }.onSuccess { scheduledMeetingList ->
                scheduledMeetingList?.let { list ->
                    list.forEach { scheduledMeetReceived ->
                        if (isMainScheduledMeeting(scheduledMeet = scheduledMeetReceived)) {
                            updateScheduledMeeting(scheduledMeetReceived = scheduledMeetReceived)
                            return@forEach
                        }
                    }
                    getScheduledMeetingUpdates()
                }

                getScheduledMeetingUpdates()
            }
        }

    /**
     * Get chat room updates
     *
     * @param chatId Chat id.
     */
    private fun getChatRoomUpdates(chatId: Long) =
        viewModelScope.launch {
            monitorChatRoomUpdatesUseCase(chatId).collectLatest { chat ->
                _uiState.update { state ->
                    with(state) {
                        val hostValue = if (chat.hasChanged(ChatRoomChange.OwnPrivilege)) {
                            Timber.d("Changes in own privilege")
                            chat.ownPrivilege == ChatRoomPermission.Moderator
                        } else {
                            isHost
                        }

                        val titleValue = if (chat.hasChanged(ChatRoomChange.Title)) {
                            Timber.d("Changes in chat title")
                            chat.title
                        } else {
                            chatTitle
                        }

                        val publicValue = if (chat.hasChanged(ChatRoomChange.ChatMode)) {
                            Timber.d("Changes in chat mode, isPublic ${chat.isPublic}")
                            chat.isPublic
                        } else {
                            isPublic
                        }

                        val retentionTimeValue =
                            if (chat.hasChanged(ChatRoomChange.RetentionTime)) {
                                Timber.d("Changes in retention time")

                                if (chat.retentionTime != Constants.DISABLED_RETENTION_TIME) chat.retentionTime
                                else null
                            } else {
                                retentionTimeSeconds
                            }

                        val waitingRoomValue = if (chat.hasChanged(ChatRoomChange.WaitingRoom)) {
                            Timber.d("Changes in waiting room")
                            if (chat.isWaitingRoom && enabledAllowNonHostAddParticipantsOption) {
                                setWaitingRoomReminderEnabled()
                            }

                            chat.isWaitingRoom
                        } else {
                            enabledWaitingRoomOption
                        }

                        val openInviteValue = if (chat.hasChanged(ChatRoomChange.OpenInvite)) {
                            Timber.d("Changes in OpenInvite")
                            if (enabledWaitingRoomOption && chat.isOpenInvite) {
                                setWaitingRoomReminderEnabled()
                            }
                            chat.isOpenInvite || isHost
                        } else {
                            isOpenInvite
                        }

                        copy(
                            isHost = hostValue,
                            chatTitle = titleValue,
                            isPublic = publicValue,
                            retentionTimeSeconds = retentionTimeValue,
                            enabledWaitingRoomOption = waitingRoomValue,
                            isOpenInvite = openInviteValue,
                            enabledAllowNonHostAddParticipantsOption = chat.isOpenInvite
                        )
                    }
                }
            }
        }

    /**
     * Update scheduled meeting
     *
     * @param scheduledMeetReceived [ChatScheduledMeeting]
     */
    private fun updateScheduledMeeting(scheduledMeetReceived: ChatScheduledMeeting) {
        _uiState.update {
            it.copy(
                scheduledMeeting = scheduledMeetReceived,
                is24HourFormat = is24HourFormat
            )
        }
    }

    /**
     * Check if is the current scheduled meeting
     *
     * @param scheduledMeet [ChatScheduledMeeting]
     * @ return True, if it's same. False if not.
     */
    private fun isSameScheduledMeeting(scheduledMeet: ChatScheduledMeeting): Boolean =
        uiState.value.chatId == scheduledMeet.chatId

    /**
     * Check if is main scheduled meeting
     *
     * @param scheduledMeet [ChatScheduledMeeting]
     * @ return True, if it's the main scheduled meeting. False if not.
     */
    private fun isMainScheduledMeeting(scheduledMeet: ChatScheduledMeeting): Boolean =
        scheduledMeet.parentSchedId == megaChatApiGateway.getChatInvalidHandle()

    /**
     * Get scheduled meeting updates
     */
    private fun getScheduledMeetingUpdates() =
        viewModelScope.launch {
            monitorScheduledMeetingUpdatesUseCase().collectLatest { scheduledMeetReceived ->
                if (!isSameScheduledMeeting(scheduledMeet = scheduledMeetReceived)) {
                    return@collectLatest
                }

                if (!isMainScheduledMeeting(scheduledMeet = scheduledMeetReceived)) {
                    return@collectLatest
                }

                scheduledMeetReceived.changes?.let { changes ->
                    changes.forEach {
                        Timber.d("Monitor scheduled meeting updated, changes $changes")
                        if (_uiState.value.scheduledMeeting == null) {
                            updateScheduledMeeting(
                                scheduledMeetReceived = scheduledMeetReceived
                            )
                            return@forEach
                        }

                        when (it) {
                            ScheduledMeetingChanges.NewScheduledMeeting ->
                                updateScheduledMeeting(
                                    scheduledMeetReceived = scheduledMeetReceived
                                )

                            ScheduledMeetingChanges.Title ->
                                _uiState.update { state ->
                                    state.copy(
                                        scheduledMeeting = state.scheduledMeeting?.copy(
                                            title = scheduledMeetReceived.title
                                        )
                                    )
                                }

                            ScheduledMeetingChanges.Description ->
                                _uiState.update { state ->
                                    state.copy(
                                        scheduledMeeting = state.scheduledMeeting?.copy(
                                            description = scheduledMeetReceived.description
                                        )
                                    )
                                }

                            ScheduledMeetingChanges.StartDate -> _uiState.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        startDateTime = scheduledMeetReceived.startDateTime,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.EndDate,
                            -> _uiState.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        startDateTime = scheduledMeetReceived.endDateTime,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.ParentScheduledMeetingId -> _uiState.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        parentSchedId = scheduledMeetReceived.parentSchedId,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.TimeZone -> _uiState.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        timezone = scheduledMeetReceived.timezone,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.Attributes -> _uiState.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        attributes = scheduledMeetReceived.attributes,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.OverrideDateTime -> _uiState.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        overrides = scheduledMeetReceived.overrides,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.ScheduledMeetingsFlags -> _uiState.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        flags = scheduledMeetReceived.flags,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.RepetitionRules -> _uiState.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        rules = scheduledMeetReceived.rules,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.CancelledFlag -> _uiState.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
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
     * Update seconds of Do not disturb mode
     *
     * @param id    Chat id.
     */
    private fun updateDndSeconds(id: Long) {
        getPushNotificationSettingManagement().pushNotificationSetting?.let { push ->
            if (push.isChatDndEnabled(id)) {
                _uiState.update {
                    it.copy(dndSeconds = push.getChatDnd(id))
                }

                return
            }
        }

        _uiState.update {
            it.copy(dndSeconds = null)
        }
    }

    /**
     * Update retention time seconds
     *
     * @param retentionTime    Retention time seconds
     */
    private fun updateRetentionTimeSeconds(retentionTime: Long) {
        if (retentionTime == Constants.DISABLED_RETENTION_TIME) {
            _uiState.update {
                it.copy(retentionTimeSeconds = null)
            }
        } else {
            _uiState.update {
                it.copy(retentionTimeSeconds = retentionTime)
            }
        }
    }

    /**
     * See more or less participants in the list.
     */
    fun onSeeMoreOrLessTap() =
        _uiState.update { state ->
            state.copy(seeMoreVisible = !state.seeMoreVisible)
        }

    /**
     * Add participants to the chat room if there is internet connection, shows an error if not.
     */
    fun onInviteParticipantsTap() {
        if (isConnected) {
            Timber.d("Add participants to the chat room")
            viewModelScope.launch {
                val contactList = getVisibleContactsUseCase()
                when {
                    contactList.isEmpty() -> {
                        _uiState.update {
                            it.copy(addParticipantsNoContactsDialog = true, openAddContact = false)
                        }
                    }

                    ChatUtil.areAllMyContactsChatParticipants(uiState.value.chatId) -> {
                        _uiState.update {
                            it.copy(
                                addParticipantsNoContactsLeftToAddDialog = true,
                                openAddContact = false
                            )
                        }
                    }

                    else -> {
                        _uiState.update {
                            it.copy(openAddContact = true)
                        }
                    }
                }
            }
        } else {
            triggerSnackbarMessage(
                getStringFromStringResMapper(
                    R.string.check_internet_connection_error
                )
            )
        }
    }

    /**
     * Send message to a participant
     */
    fun onSendMsgTap() =
        uiState.value.selected?.let { participant ->
            if (isConnected) {
                viewModelScope.launch {
                    runCatching {
                        startConversationUseCase(
                            isGroup = false,
                            userHandles = listOf(participant.handle)
                        )
                    }.onFailure { exception ->
                        Timber.e(exception)
                        triggerSnackbarMessage(
                            getStringFromStringResMapper(
                                R.string.general_text_error
                            )
                        )
                    }.onSuccess { chatId ->
                        Timber.d("Open chat room")
                        openChatRoom(chatId)
                    }
                }
            } else {
                triggerSnackbarMessage(
                    getStringFromStringResMapper(
                        R.string.check_internet_connection_error
                    )
                )
            }
        }


    /**
     * Start call with a participant
     */
    fun onStartCallTap() =
        uiState.value.selected?.let { participant ->
            if (isConnected) {
                viewModelScope.launch {
                    runCatching {
                        startConversationUseCase(
                            isGroup = false,
                            userHandles = listOf(participant.handle)
                        )
                    }.onFailure { exception ->
                        Timber.d(exception)
                        triggerSnackbarMessage(
                            getStringFromStringResMapper(
                                R.string.general_text_error
                            )
                        )
                    }.onSuccess { chatCallId ->
                        openOrStartChatCall(chatCallId)
                    }
                }
            } else {
                triggerSnackbarMessage(
                    getStringFromStringResMapper(
                        R.string.check_internet_connection_error
                    )
                )
            }
        }

    /**
     * Open call or start a new call and open it
     *
     * @param chatCallId chat id
     */
    private fun openOrStartChatCall(chatCallId: Long) {
        viewModelScope.launch {
            setChatVideoInDeviceUseCase()
            runCatching { openOrStartCallUseCase(chatCallId, audio = false, video = false) }
                .onSuccess { call ->
                    call?.let {
                        Timber.d("Call started")
                        CallUtil.addChecksForACall(call.chatId, false)
                        if (call.isOutgoing) {
                            chatManagement.setRequestSentCall(call.callId, true)
                        }
                        passcodeManagement.showPasscodeScreen = true
                        getInstance().openCallService(chatCallId)
                        openChatCall(call.chatId)
                    }
                }.onFailure { Timber.e("Exception opening or starting call: $it") }
        }
    }

    /**
     * Change permissions
     */
    fun onChangePermissionsTap() =
        _uiState.value.selected?.let {
            showChangePermissionsDialog(it.privilege)
        }

    /**
     * Show or hide Remove participant dialog
     *
     * @param shouldShowDialog True,show dialog.
     */
    fun onRemoveParticipantTap(shouldShowDialog: Boolean) =
        _uiState.update {
            it.copy(openRemoveParticipantDialog = shouldShowDialog)
        }

    /**
     * Leave group chat button clicked
     */
    fun onLeaveGroupTap() =
        _uiState.update { state ->
            state.copy(leaveGroupDialog = !state.leaveGroupDialog)
        }

    /**
     * Invite contact
     */
    fun onInviteContactTap() =
        _uiState.value.selected?.let { participant ->
            participant.email?.let { email ->
                viewModelScope.launch {
                    runCatching {
                        inviteContactWithEmailUseCase(email)
                    }.onFailure { exception ->
                        Timber.e(exception)
                        triggerSnackbarMessage(
                            getStringFromStringResMapper(
                                R.string.general_error
                            )
                        )
                    }.onSuccess { request ->
                        when (request) {
                            InviteContactRequest.Sent -> triggerSnackbarMessage(
                                getStringFromStringResMapper(
                                    R.string.context_contact_request_sent,
                                    email
                                )
                            )

                            InviteContactRequest.Resent -> triggerSnackbarMessage(
                                getStringFromStringResMapper(
                                    R.string.context_contact_invitation_resent
                                )
                            )

                            InviteContactRequest.Deleted -> triggerSnackbarMessage(
                                getStringFromStringResMapper(
                                    R.string.context_contact_invitation_deleted
                                )
                            )

                            InviteContactRequest.AlreadySent -> triggerSnackbarMessage(
                                getStringFromStringResMapper(
                                    R.string.invite_not_sent_already_sent,
                                    email
                                )
                            )

                            InviteContactRequest.AlreadyContact -> triggerSnackbarMessage(
                                getStringFromStringResMapper(
                                    R.string.context_contact_already_exists,
                                    email
                                )
                            )

                            InviteContactRequest.InvalidEmail -> triggerSnackbarMessage(
                                getStringFromStringResMapper(
                                    R.string.error_own_email_as_contact
                                )
                            )

                            else -> triggerSnackbarMessage(
                                getStringFromStringResMapper(
                                    R.string.general_error
                                )
                            )
                        }
                    }
                }
            }
        }

    /**
     * Remove open add contact screen
     */
    fun removeAddContact() =
        _uiState.update {
            it.copy(openAddContact = null)
        }

    /**
     * Invite participants to the chat room
     *
     * @param contacts list of contacts
     */
    fun inviteToChat(contacts: ArrayList<String>) {
        Timber.d("Invite participants")
        viewModelScope.launch {
            inviteToChat(_uiState.value.chatId, contacts)
        }
        triggerSnackbarMessage(
            getStringFromStringResMapper(
                R.string.invite_sent
            )
        )
    }

    /**
     * Dismiss alert dialogs
     */
    fun dismissDialog() =
        _uiState.update { state ->
            state.copy(
                leaveGroupDialog = false,
                addParticipantsNoContactsDialog = false,
                addParticipantsNoContactsLeftToAddDialog = false
            )
        }

    /**
     * Finish activity
     */
    private fun finishActivity() =
        _uiState.update { state ->
            state.copy(finish = true)
        }

    /**
     * Leave chat
     */
    fun leaveChat() =
        viewModelScope.launch {
            runCatching {
                chatManagement.addLeavingChatId(uiState.value.chatId)
                leaveChatUseCase(uiState.value.chatId)
            }.onFailure { exception ->
                Timber.e(exception)
                chatManagement.removeLeavingChatId(uiState.value.chatId)
                dismissDialog()
                triggerSnackbarMessage(
                    getStringFromStringResMapper(
                        R.string.general_error
                    )
                )
            }.onSuccess {
                Timber.d("Chat left ")
                chatManagement.removeLeavingChatId(uiState.value.chatId)
                dismissDialog()
                finishActivity()
            }
        }

    /**
     * Open bottom panel option of a participant.
     *
     * @param participant [ChatParticipant]
     */
    fun onParticipantTap(participant: ChatParticipant) =
        _uiState.update {
            it.copy(selected = participant)
        }

    /**
     * Enable or disable the option Allow non-host add participants to the chat room if there is internet connection, shows an error if not.
     */
    fun onAllowAddParticipantsTap() {
        if (isConnected) {
            Timber.d("Update option Allow non-host add participants to the chat room")
            viewModelScope.launch {
                runCatching {
                    setOpenInviteWithChatIdUseCase(uiState.value.chatId)
                }.onFailure { exception ->
                    Timber.e(exception)
                    triggerSnackbarMessage(
                        getStringFromStringResMapper(
                            R.string.general_text_error
                        )
                    )
                }.onSuccess { isAllowAddParticipantsEnabled ->
                    _uiState.update { state ->
                        state.copy(
                            isOpenInvite = isAllowAddParticipantsEnabled || state.isHost,
                            enabledAllowNonHostAddParticipantsOption = isAllowAddParticipantsEnabled,
                        )
                    }

                    if (uiState.value.enabledWaitingRoomOption && isAllowAddParticipantsEnabled) {
                        setWaitingRoomReminderEnabled()
                    }
                }
            }
        } else {
            triggerSnackbarMessage(
                getStringFromStringResMapper(
                    R.string.check_internet_connection_error
                )
            )
        }
    }

    /**
     * Enable or disable waiting room option
     */
    fun setWaitingRoom() =
        viewModelScope.launch {
            val newValueForWaitingRoomOption = !uiState.value.enabledWaitingRoomOption
            runCatching {
                setWaitingRoomUseCase(uiState.value.chatId, newValueForWaitingRoomOption)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess {
                _uiState.update { state ->
                    state.copy(
                        enabledWaitingRoomOption = newValueForWaitingRoomOption
                    )
                }

                if (newValueForWaitingRoomOption && uiState.value.enabledAllowNonHostAddParticipantsOption) {
                    setWaitingRoomReminderEnabled()
                }
            }
        }

    /**
     * Enable waiting room reminder
     */
    private fun setWaitingRoomReminderEnabled() = viewModelScope.launch {
        runCatching {
            setWaitingRoomRemindersUseCase(WaitingRoomReminders.Enabled)
        }
    }

    /**
     * Enable encrypted key rotation if there is internet connection, shows an error if not.
     */
    fun enableEncryptedKeyRotation() {
        if (_uiState.value.participantItemList.size > MAX_PARTICIPANTS_TO_MAKE_THE_CHAT_PRIVATE) {
            triggerSnackbarMessage(
                getStringFromStringResMapper(
                    R.string.warning_make_chat_private
                )
            )
        } else {
            viewModelScope.launch {
                runCatching {
                    getPublicChatToPrivate(uiState.value.chatId)
                }.onFailure { exception ->
                    Timber.e(exception)
                    triggerSnackbarMessage(
                        getStringFromStringResMapper(
                            R.string.general_error
                        )
                    )
                }.onSuccess { _ ->
                    _uiState.update { it.copy(isPublic = false) }
                }
            }
        }
    }

    /**
     * Open change selected participant permission
     *
     * @param selectedParticipantPermission [ChatRoomPermission]
     */
    fun showChangePermissionsDialog(selectedParticipantPermission: ChatRoomPermission?) {
        _uiState.update { it.copy(showChangePermissionsDialog = selectedParticipantPermission) }
    }

    /**
     * Open chat room
     *
     * @param chatId Chat id.
     */
    fun openChatRoom(chatId: Long?) =
        _uiState.update { it.copy(openChatRoom = chatId) }

    /**
     * Open chat call
     *
     * @param chatCallId Chat id.
     */
    fun openChatCall(chatCallId: Long?) {
        _uiState.update { it.copy(openChatCall = chatCallId) }
    }

    /**
     * Update participant permissions
     *
     * @param permission [ChatRoomPermission]
     */
    fun updateParticipantPermissions(permission: ChatRoomPermission) =
        _uiState.value.selected?.let { participant ->
            viewModelScope.launch {
                runCatching {
                    updateChatPermissionsUseCase(
                        chatId = uiState.value.chatId,
                        nodeId = NodeId(participant.handle),
                        permission = permission
                    )
                }.onFailure { exception ->
                    Timber.e(exception)
                    triggerSnackbarMessage(
                        getStringFromStringResMapper(
                            R.string.general_error
                        )
                    )
                }.onSuccess {}
            }
        }

    /**
     * Remove selected participant from chat
     */
    fun removeSelectedParticipant() =
        _uiState.value.selected?.let { participant ->
            viewModelScope.launch {
                runCatching {
                    removeFromChat(uiState.value.chatId, participant.handle)
                }.onFailure { exception ->
                    Timber.e(exception)
                    triggerSnackbarMessage(getStringFromStringResMapper(R.string.general_error))
                }.onSuccess {
                    triggerSnackbarMessage(
                        getStringFromStringResMapper(
                            R.string.remove_participant_success
                        )
                    )
                }
            }
        }

    /**
     * Scheduled meeting updated
     */
    fun scheduledMeetingUpdated() {
        triggerSnackbarMessage(getStringFromStringResMapper(R.string.meetings_edit_scheduled_meeting_success_snackbar))
        getChat()
    }

    /**
     * Open send to screen
     */
    fun openSendToChat(shouldOpen: Boolean) {
        _uiState.update { it.copy(openSendToChat = shouldOpen) }
    }

    /**
     * Check whether the initial bar should be displayed
     */
    fun checkInitialSnackbar(shouldBeShown: Boolean) {
        if (shouldBeShown) {
            triggerSnackbarMessage(getStringFromStringResMapper(R.string.meetings_scheduled_meeting_info_snackbar_creating_scheduled_meeting_success))
        }
    }

    /**
     * Trigger event to show Snackbar message
     *
     * @param message     Content for snack bar
     */
    fun triggerSnackbarMessage(message: String) =
        _uiState.update { it.copy(snackbarMsg = triggered(message)) }

    /**
     * Reset and notify that snackbarMessage is consumed
     */
    fun onSnackbarMessageConsumed() =
        _uiState.update {
            it.copy(snackbarMsg = consumed())
        }

    /**
     * Get my full name
     */
    private fun getMyFullName() = viewModelScope.launch {
        runCatching {
            getMyFullNameUseCase()
        }.onSuccess {
            it?.apply {
                _uiState.update { state ->
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
     * monitor chat call updates
     */
    private fun monitorSFUServerUpgrade() {
        monitorSFUServerUpgradeJob?.cancel()
        monitorSFUServerUpgradeJob = viewModelScope.launch {
            monitorSFUServerUpgradeUseCase()
                .catch {
                    Timber.e(it)
                }
                .collect { shouldUpgrade ->
                    if (shouldUpgrade) {
                        showForceUpdateDialog()
                    }
                }
        }
    }

    private fun showForceUpdateDialog() {
        _uiState.update { it.copy(showForceUpdateDialog = true) }
    }

    /**
     * Set to false to hide the dialog
     */
    fun onForceUpdateDialogDismissed() {
        _uiState.update { it.copy(showForceUpdateDialog = false) }
    }

    companion object {
        private const val MAX_PARTICIPANTS_TO_MAKE_THE_CHAT_PRIVATE = 100
    }
}
