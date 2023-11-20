package mega.privacy.android.app.presentation.meeting

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.MegaApplication.Companion.getInstance
import mega.privacy.android.app.MegaApplication.Companion.getPushNotificationSettingManagement
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.constants.BroadcastConstants.ACTION_UPDATE_RETENTION_TIME
import mega.privacy.android.app.constants.BroadcastConstants.RETENTION_TIME
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.presentation.meeting.model.ScheduledMeetingInfoState
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.ChatUtil
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.chat.ChatScheduledMeeting
import mega.privacy.android.domain.entity.chat.ScheduledMeetingChanges
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import mega.privacy.android.domain.entity.meeting.WaitingRoomReminders
import mega.privacy.android.domain.usecase.GetChatParticipants
import mega.privacy.android.domain.usecase.GetChatRoom
import mega.privacy.android.domain.usecase.GetVisibleContactsUseCase
import mega.privacy.android.domain.usecase.InviteContact
import mega.privacy.android.domain.usecase.InviteToChat
import mega.privacy.android.domain.usecase.MonitorChatRoomUpdates
import mega.privacy.android.domain.usecase.RemoveFromChat
import mega.privacy.android.domain.usecase.SetOpenInvite
import mega.privacy.android.domain.usecase.SetPublicChatToPrivate
import mega.privacy.android.domain.usecase.UpdateChatPermissions
import mega.privacy.android.domain.usecase.chat.LeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.StartConversationUseCase
import mega.privacy.android.domain.usecase.meeting.GetScheduledMeetingByChat
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.OpenOrStartCallUseCase
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
 * @property getChatRoom                                    [GetChatRoom]
 * @property getChatParticipants                            [GetChatParticipants]
 * @property getScheduledMeetingByChat                      [GetScheduledMeetingByChat]
 * @property getVisibleContactsUseCase                      [GetVisibleContactsUseCase]
 * @property inviteToChat                                   [InviteToChat]
 * @property leaveChatUseCase                               [LeaveChatUseCase]
 * @property removeFromChat                                 [RemoveFromChat]
 * @property inviteContact                                  [InviteContact]
 * @property setOpenInvite                                  [SetOpenInvite]
 * @property updateChatPermissions                          [UpdateChatPermissions]
 * @property getPublicChatToPrivate                         [SetPublicChatToPrivate]
 * @property passcodeManagement                             [PasscodeManagement]
 * @property chatManagement                                 [ChatManagement]
 * @property startConversationUseCase                       [StartConversationUseCase]
 * @property openOrStartCallUseCase                         [OpenOrStartCallUseCase]
 * @property monitorScheduledMeetingUpdatesUseCase          [MonitorScheduledMeetingUpdatesUseCase]
 * @property monitorConnectivityUseCase                     [MonitorConnectivityUseCase]
 * @property monitorChatRoomUpdates                         [MonitorChatRoomUpdates]
 * @property monitorUpdatePushNotificationSettingsUseCase   [MonitorUpdatePushNotificationSettingsUseCase]
 * @property setChatVideoInDeviceUseCase                    [SetChatVideoInDeviceUseCase]
 * @property deviceGateway                                  [DeviceGateway]
 * @property setWaitingRoomUseCase                          [SetWaitingRoomUseCase]
 * @property setWaitingRoomRemindersUseCase                 [SetWaitingRoomRemindersUseCase]
 * @property getStringFromStringResMapper                   [GetStringFromStringResMapper]
 * @property state                    Current view state as [ScheduledMeetingInfoState]

 */
@HiltViewModel
class ScheduledMeetingInfoViewModel @Inject constructor(
    private val getChatRoom: GetChatRoom,
    private val getChatParticipants: GetChatParticipants,
    private val getScheduledMeetingByChat: GetScheduledMeetingByChat,
    private val getVisibleContactsUseCase: GetVisibleContactsUseCase,
    private val inviteToChat: InviteToChat,
    private val leaveChatUseCase: LeaveChatUseCase,
    private val removeFromChat: RemoveFromChat,
    private val inviteContact: InviteContact,
    private val setOpenInvite: SetOpenInvite,
    private val updateChatPermissions: UpdateChatPermissions,
    private val getPublicChatToPrivate: SetPublicChatToPrivate,
    private val passcodeManagement: PasscodeManagement,
    private val chatManagement: ChatManagement,
    private val startConversationUseCase: StartConversationUseCase,
    private val openOrStartCallUseCase: OpenOrStartCallUseCase,
    private val monitorScheduledMeetingUpdatesUseCase: MonitorScheduledMeetingUpdatesUseCase,
    private val monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val isConnectedToInternetUseCase: IsConnectedToInternetUseCase,
    private val monitorChatRoomUpdates: MonitorChatRoomUpdates,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val deviceGateway: DeviceGateway,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val setWaitingRoomUseCase: SetWaitingRoomUseCase,
    private val setWaitingRoomRemindersUseCase: SetWaitingRoomRemindersUseCase,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
) : ViewModel() {

    private val _state = MutableStateFlow(ScheduledMeetingInfoState())
    val state: StateFlow<ScheduledMeetingInfoState> = _state

    private val is24HourFormat by lazy { deviceGateway.is24HourFormat() }

    private var scheduledMeetingId: Long = megaChatApiGateway.getChatInvalidHandle()

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
        monitorMutedChatsUpdates()
    }

    /**
     * Sets chat id and scheduled meeting id
     *
     * @param newChatId                 Chat id.
     * @param newScheduledMeetingId     Scheduled meeting id.
     */
    fun setChatId(newChatId: Long, newScheduledMeetingId: Long) {
        if (newChatId != megaChatApiGateway.getChatInvalidHandle() && newChatId != state.value.chatId) {
            _state.update {
                it.copy(
                    chatId = newChatId
                )
            }
            scheduledMeetingId = newScheduledMeetingId
            getChat()
            getScheduledMeeting()
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
                finishActivity()
            }.onSuccess { chat ->
                Timber.d("Chat room exists")
                chat?.apply {
                    if (isActive) {
                        Timber.d("Chat room is active")
                        _state.update { state ->
                            state.copy(
                                chatId = chatId,
                                chatTitle = title,
                                isHost = ownPrivilege == ChatRoomPermission.Moderator,
                                isOpenInvite = isOpenInvite || ownPrivilege == ChatRoomPermission.Moderator,
                                enabledAllowNonHostAddParticipantsOption = isOpenInvite,
                                enabledWaitingRoomOption = isWaitingRoom,
                                isPublic = isPublic
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
     * Monitor muted chats updates
     */
    private fun monitorMutedChatsUpdates() = viewModelScope.launch {
        monitorUpdatePushNotificationSettingsUseCase().collectLatest {
            updateDndSeconds(state.value.chatId)
        }
    }

    /**
     * Load all chat participants
     */
    private fun loadAllChatParticipants() = viewModelScope.launch {
        runCatching {
            getChatParticipants(state.value.chatId)
                .catch { exception ->
                    Timber.e(exception)
                }
                .collectLatest { list ->
                    Timber.d("Updated list of participants: list ${list.size}")
                    _state.update {
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
        _state.value.participantItemList.let { list ->
            _state.update {
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
                getScheduledMeetingByChat(state.value.chatId)
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
            monitorChatRoomUpdates(chatId).collectLatest { chat ->
                _state.update { state ->
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
                                getInstance().sendBroadcast(
                                    Intent(ACTION_UPDATE_RETENTION_TIME)
                                        .putExtra(RETENTION_TIME, chat.retentionTime)
                                        .setPackage(getInstance().applicationContext.packageName)
                                )

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
        _state.update {
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
        state.value.chatId == scheduledMeet.chatId

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
                        if (_state.value.scheduledMeeting == null) {
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
                                _state.update { state ->
                                    state.copy(
                                        scheduledMeeting = state.scheduledMeeting?.copy(
                                            title = scheduledMeetReceived.title
                                        )
                                    )
                                }

                            ScheduledMeetingChanges.Description ->
                                _state.update { state ->
                                    state.copy(
                                        scheduledMeeting = state.scheduledMeeting?.copy(
                                            description = scheduledMeetReceived.description
                                        )
                                    )
                                }

                            ScheduledMeetingChanges.StartDate -> _state.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        startDateTime = scheduledMeetReceived.startDateTime,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.EndDate,
                            -> _state.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        startDateTime = scheduledMeetReceived.endDateTime,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.ParentScheduledMeetingId -> _state.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        parentSchedId = scheduledMeetReceived.parentSchedId,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.TimeZone -> _state.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        timezone = scheduledMeetReceived.timezone,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.Attributes -> _state.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        attributes = scheduledMeetReceived.attributes,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.OverrideDateTime -> _state.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        overrides = scheduledMeetReceived.overrides,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.ScheduledMeetingsFlags -> _state.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        flags = scheduledMeetReceived.flags,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.RepetitionRules -> _state.update { state ->
                                state.copy(
                                    scheduledMeeting = state.scheduledMeeting?.copy(
                                        rules = scheduledMeetReceived.rules,
                                    )
                                )
                            }

                            ScheduledMeetingChanges.CancelledFlag -> _state.update { state ->
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
                _state.update {
                    it.copy(dndSeconds = push.getChatDnd(id))
                }

                return
            }
        }

        _state.update {
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
            _state.update {
                it.copy(retentionTimeSeconds = null)
            }
        } else {
            _state.update {
                it.copy(retentionTimeSeconds = retentionTime)
            }
        }
    }

    /**
     * See more or less participants in the list.
     */
    fun onSeeMoreOrLessTap() =
        _state.update { state ->
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
                        _state.update {
                            it.copy(addParticipantsNoContactsDialog = true, openAddContact = false)
                        }
                    }

                    ChatUtil.areAllMyContactsChatParticipants(state.value.chatId) -> {
                        _state.update {
                            it.copy(
                                addParticipantsNoContactsLeftToAddDialog = true,
                                openAddContact = false
                            )
                        }
                    }

                    else -> {
                        _state.update {
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
        state.value.selected?.let { participant ->
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
        state.value.selected?.let { participant ->
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
            runCatching { openOrStartCallUseCase(chatCallId, video = false) }
                .onSuccess { call ->
                    call?.let {
                        Timber.d("Call started")
                        MegaApplication.isWaitingForCall = false
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
        _state.value.selected?.let {
            showChangePermissionsDialog(it.privilege)
        }

    /**
     * Show or hide Remove participant dialog
     *
     * @param shouldShowDialog True,show dialog.
     */
    fun onRemoveParticipantTap(shouldShowDialog: Boolean) =
        _state.update {
            it.copy(openRemoveParticipantDialog = shouldShowDialog)
        }

    /**
     * Leave group chat button clicked
     */
    fun onLeaveGroupTap() =
        _state.update { state ->
            state.copy(leaveGroupDialog = !state.leaveGroupDialog)
        }

    /**
     * Invite contact
     */
    fun onInviteContactTap() =
        _state.value.selected?.let { participant ->
            participant.email?.let { email ->
                viewModelScope.launch {
                    runCatching {
                        inviteContact(email)
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
        _state.update {
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
            inviteToChat(_state.value.chatId, contacts)
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
        _state.update { state ->
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
        _state.update { state ->
            state.copy(finish = true)
        }

    /**
     * Leave chat
     */
    fun leaveChat() =
        viewModelScope.launch {
            runCatching {
                chatManagement.addLeavingChatId(state.value.chatId)
                leaveChatUseCase(state.value.chatId)
            }.onFailure { exception ->
                Timber.e(exception)
                chatManagement.removeLeavingChatId(state.value.chatId)
                dismissDialog()
                triggerSnackbarMessage(
                    getStringFromStringResMapper(
                        R.string.general_error
                    )
                )
            }.onSuccess {
                Timber.d("Chat left ")
                chatManagement.removeLeavingChatId(state.value.chatId)
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
        _state.update {
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
                    setOpenInvite(state.value.chatId)
                }.onFailure { exception ->
                    Timber.e(exception)
                    triggerSnackbarMessage(
                        getStringFromStringResMapper(
                            R.string.general_text_error
                        )
                    )
                }.onSuccess { isAllowAddParticipantsEnabled ->
                    _state.update { state ->
                        state.copy(
                            isOpenInvite = isAllowAddParticipantsEnabled || state.isHost,
                            enabledAllowNonHostAddParticipantsOption = isAllowAddParticipantsEnabled,
                        )
                    }

                    if (state.value.enabledWaitingRoomOption && isAllowAddParticipantsEnabled) {
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
            val newValueForWaitingRoomOption = !state.value.enabledWaitingRoomOption
            runCatching {
                setWaitingRoomUseCase(state.value.chatId, newValueForWaitingRoomOption)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess {
                _state.update { state ->
                    state.copy(
                        enabledWaitingRoomOption = newValueForWaitingRoomOption
                    )
                }

                if (newValueForWaitingRoomOption && state.value.enabledAllowNonHostAddParticipantsOption) {
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
        if (_state.value.participantItemList.size > MAX_PARTICIPANTS_TO_MAKE_THE_CHAT_PRIVATE) {
            triggerSnackbarMessage(
                getStringFromStringResMapper(
                    R.string.warning_make_chat_private
                )
            )
        } else {
            viewModelScope.launch {
                runCatching {
                    getPublicChatToPrivate(state.value.chatId)
                }.onFailure { exception ->
                    Timber.e(exception)
                    triggerSnackbarMessage(
                        getStringFromStringResMapper(
                            R.string.general_error
                        )
                    )
                }.onSuccess { _ ->
                    _state.update { it.copy(isPublic = false) }
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
        _state.update { it.copy(showChangePermissionsDialog = selectedParticipantPermission) }
    }

    /**
     * Open chat room
     *
     * @param chatId Chat id.
     */
    fun openChatRoom(chatId: Long?) =
        _state.update { it.copy(openChatRoom = chatId) }

    /**
     * Open chat call
     *
     * @param chatCallId Chat id.
     */
    fun openChatCall(chatCallId: Long?) {
        _state.update { it.copy(openChatCall = chatCallId) }
    }

    /**
     * Update participant permissions
     *
     * @param permission [ChatRoomPermission]
     */
    fun updateParticipantPermissions(permission: ChatRoomPermission) =
        _state.value.selected?.let { participant ->
            viewModelScope.launch {
                runCatching {
                    updateChatPermissions(state.value.chatId, participant.handle, permission)
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
        _state.value.selected?.let { participant ->
            viewModelScope.launch {
                runCatching {
                    removeFromChat(state.value.chatId, participant.handle)
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
        _state.update { it.copy(openSendToChat = shouldOpen) }
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
        _state.update { it.copy(snackbarMsg = triggered(message)) }

    /**
     * Reset and notify that snackbarMessage is consumed
     */
    fun onSnackbarMessageConsumed() =
        _state.update {
            it.copy(snackbarMsg = consumed())
        }


    companion object {
        private const val MAX_PARTICIPANTS_TO_MAKE_THE_CHAT_PRIVATE = 100
    }
}
