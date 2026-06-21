package mega.privacy.android.app.presentation.chat.groupInfo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.presentation.chat.groupInfo.model.ArchiveChatResult
import mega.privacy.android.app.presentation.chat.groupInfo.model.GroupInfoState
import mega.privacy.android.app.presentation.meeting.model.MeetingState.Companion.FREE_PLAN_PARTICIPANTS_LIMIT
import mega.privacy.android.app.usecase.chat.SetChatVideoInDeviceUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.CallUtil.openMeetingWithAudioOrVideo
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.chat.ChatRoom
import mega.privacy.android.domain.entity.chat.ChatRoomChange
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.statistics.EndCallForAll
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.GetChatRoomUseCase
import mega.privacy.android.domain.usecase.SetOpenInviteWithChatIdUseCase
import mega.privacy.android.domain.usecase.call.MonitorSFUServerUpgradeUseCase
import mega.privacy.android.domain.usecase.call.StartCallUseCase
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.chat.BroadcastChatArchivedUseCase
import mega.privacy.android.domain.usecase.chat.BroadcastLeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.EndCallUseCase
import mega.privacy.android.domain.usecase.chat.Get1On1ChatIdUseCase
import mega.privacy.android.domain.usecase.chat.MonitorCallInChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorChatRoomUpdatesUseCase
import mega.privacy.android.domain.usecase.chat.RemoveParticipantFromChatUseCase
import mega.privacy.android.domain.usecase.chat.SetChatTitleUseCase
import mega.privacy.android.domain.usecase.chat.UpdateChatPermissionsUseCase
import mega.privacy.android.domain.usecase.chat.participants.MonitorChatParticipantsUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.SendStatisticsMeetingsUseCase
import mega.privacy.android.domain.usecase.network.MonitorConnectivityUseCase
import mega.privacy.android.domain.usecase.setting.MonitorUpdatePushNotificationSettingsUseCase
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import javax.inject.Inject

/**
 * GroupChatInfoActivity view model.
 *
 * @property setOpenInviteWithChatIdUseCase                 [SetOpenInviteWithChatIdUseCase]
 * @property startCallUseCase                               [StartCallUseCase]
 * @property chatApiGateway                                 [MegaChatApiGateway]
 * @property setChatVideoInDeviceUseCase                    [SetChatVideoInDeviceUseCase]
 * @property chatManagement                                 [ChatManagement]
 * @property endCallUseCase                                 [EndCallUseCase]
 * @property sendStatisticsMeetingsUseCase                  [SendStatisticsMeetingsUseCase]
 * @property monitorUpdatePushNotificationSettingsUseCase   [MonitorUpdatePushNotificationSettingsUseCase]
 * @property broadcastChatArchivedUseCase                   [BroadcastChatArchivedUseCase]
 * @property broadcastLeaveChatUseCase                      [BroadcastLeaveChatUseCase]
 * @property get1On1ChatIdUseCase                           [Get1On1ChatIdUseCase]
 * @property monitorChatParticipantsUseCase                 [MonitorChatParticipantsUseCase]
 * @property state                                          Current view state as [GroupInfoState]
 */
@HiltViewModel
class GroupChatInfoViewModel @Inject constructor(
    private val setOpenInviteWithChatIdUseCase: SetOpenInviteWithChatIdUseCase,
    monitorConnectivityUseCase: MonitorConnectivityUseCase,
    private val startCallUseCase: StartCallUseCase,
    private val chatApiGateway: MegaChatApiGateway,
    private val setChatVideoInDeviceUseCase: SetChatVideoInDeviceUseCase,
    private val chatManagement: ChatManagement,
    private val endCallUseCase: EndCallUseCase,
    private val sendStatisticsMeetingsUseCase: SendStatisticsMeetingsUseCase,
    private val monitorUpdatePushNotificationSettingsUseCase: MonitorUpdatePushNotificationSettingsUseCase,
    private val broadcastChatArchivedUseCase: BroadcastChatArchivedUseCase,
    private val broadcastLeaveChatUseCase: BroadcastLeaveChatUseCase,
    private val monitorSFUServerUpgradeUseCase: MonitorSFUServerUpgradeUseCase,
    private val get1On1ChatIdUseCase: Get1On1ChatIdUseCase,
    private val monitorCallInChatUseCase: MonitorCallInChatUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val monitorChatRoomUpdatesUseCase: MonitorChatRoomUpdatesUseCase,
    private val getChatRoomUseCase: GetChatRoomUseCase,
    private val monitorChatParticipantsUseCase: MonitorChatParticipantsUseCase,
    private val setChatTitleUseCase: SetChatTitleUseCase,
    private val archiveChatUseCase: ArchiveChatUseCase,
    private val updateChatPermissionsUseCase: UpdateChatPermissionsUseCase,
    private val removeParticipantFromChatUseCase: RemoveParticipantFromChatUseCase,
) : ViewModel() {

    /**
     * private UI state
     */
    private val _state = MutableStateFlow(GroupInfoState())

    private var monitorChatRoomUpdatesJob: Job? = null
    private var monitorSFUServerUpgradeJob: Job? = null
    private var monitorChatParticipantsUpdatesJob: Job? = null
    private var monitorChatCallJob: Job? = null

    /**
     * UI State GroupChatInfo
     * Flow of [GroupInfoState]
     */
    val state = _state.asStateFlow()

    private val isConnected =
        monitorConnectivityUseCase().stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            monitorUpdatePushNotificationSettingsUseCase().collect {
                _state.update { it.copy(isPushNotificationSettingsUpdatedEvent = true) }
            }
        }
        monitorSFUServerUpgrade()
        getApiFeatureFlag()
    }

    private fun monitorChatUpdates(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                getChatRoomUseCase(chatId)
            }.onSuccess { chat ->
                _state.update { state -> state.copy(chatRoom = chat) }
            }.onFailure {
                Timber.e(it)
            }
        }
        monitorChatRoomUpdatesJob?.cancel()
        monitorChatRoomUpdatesJob = viewModelScope.launch {
            monitorChatRoomUpdatesUseCase(chatId)
                .catch {
                    Timber.e(it)
                }
                .collectLatest { chat ->
                    _state.update { state -> state.copy(chatRoom = chat) }
                    when {
                        chat.hasChanged(ChatRoomChange.OpenInvite) -> {
                            _state.update { state -> state.copy(resultSetOpenInvite = chat.isOpenInvite) }
                        }

                        chat.hasChanged(ChatRoomChange.RetentionTime) -> {
                            _state.update { state -> state.copy(retentionTime = chat.retentionTime) }
                        }
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
            monitorCallInChat()
            monitorChatParticipantsUpdates()
            monitorChatUpdates(newChatId)
        }
    }

    /**
     * Allow add participants
     */
    fun onAllowAddParticipantsTap(chatId: Long) {
        if (isConnected.value) {
            viewModelScope.launch {
                runCatching {
                    setOpenInviteWithChatIdUseCase(chatId)
                }.onFailure { exception ->
                    Timber.e(exception)
                    _state.update { it.copy(error = sharedR.string.general_text_error) }
                }.onSuccess { result ->
                    _state.update {
                        it.copy(resultSetOpenInvite = result)
                    }
                }
            }
        } else {
            _state.update { it.copy(error = R.string.check_internet_connection_error) }
        }
    }

    /**
     * Changes the title of the chat room.
     *
     * @param chatId The chat id.
     * @param title  The new chat room title.
     */
    fun setChatTitle(chatId: Long, title: String) {
        viewModelScope.launch {
            runCatching {
                setChatTitleUseCase(chatId, title)
            }.onFailure { exception ->
                Timber.e(exception, "Error changing chat title")
            }
        }
    }

    /**
     * Archives or unarchives the current chat room. The archive state is toggled
     * based on the chat room's current state.
     */
    fun archiveChat() {
        val chatRoom = _state.value.chatRoom
        if (chatRoom == null) {
            Timber.w("Unable to archive chat: chat room is not available")
            return
        }
        val archive = !chatRoom.isArchived
        val chatTitle = buildChatTitleForFeedback(chatRoom)
        viewModelScope.launch {
            runCatching {
                archiveChatUseCase(chatRoom.chatId, archive)
            }.onSuccess {
                if (archive) {
                    broadcastChatArchivedUseCase(chatTitle)
                }
                _state.update {
                    it.copy(
                        archiveChatResult = ArchiveChatResult(
                            success = true,
                            isArchive = archive,
                            chatTitle = chatTitle,
                        )
                    )
                }
            }.onFailure { exception ->
                Timber.e(exception, "Error archiving chat")
                _state.update {
                    it.copy(
                        archiveChatResult = ArchiveChatResult(
                            success = false,
                            isArchive = archive,
                            chatTitle = chatTitle,
                        )
                    )
                }
            }
        }
    }

    /**
     * Builds the chat title used in archive/unarchive feedback messages, truncating
     * long titles and wrapping non-custom group titles in quotes.
     */
    private fun buildChatTitleForFeedback(chatRoom: ChatRoom): String {
        var chatTitle = chatRoom.title
        if (chatTitle.length > MAX_LENGTH_CHAT_TITLE) {
            chatTitle = chatTitle.substring(0, 59) + "..."
        }
        if (chatTitle.isNotEmpty() && chatRoom.isGroup && !chatRoom.hasCustomTitle) {
            chatTitle = "\"$chatTitle\""
        }
        return chatTitle
    }

    /**
     * Consumes the archive chat result event.
     */
    fun onConsumeArchiveChatResult() {
        _state.update { it.copy(archiveChatResult = null) }
    }

    /**
     * Updates the permissions of a participant in the chat room.
     *
     * @param chatId     The chat id.
     * @param handle     The participant handle.
     * @param permission The new [ChatRoomPermission].
     */
    fun updateChatPermissions(chatId: Long, handle: Long, permission: ChatRoomPermission) {
        viewModelScope.launch {
            runCatching {
                updateChatPermissionsUseCase(chatId, NodeId(handle), permission)
            }.onFailure { exception ->
                Timber.e(exception, "Error updating chat permissions")
            }
        }
    }

    /**
     * Removes a participant from the chat room.
     *
     * @param chatId The chat id.
     * @param handle The participant handle.
     */
    fun removeParticipant(chatId: Long, handle: Long) {
        viewModelScope.launch {
            runCatching {
                removeParticipantFromChatUseCase(chatId, handle)
            }.onSuccess {
                _state.update { it.copy(removeParticipantSuccess = true) }
            }.onFailure { exception ->
                Timber.e(exception, "Error removing participant from chat")
                _state.update { it.copy(removeParticipantSuccess = false) }
            }
        }
    }

    /**
     * Consumes the remove participant result event.
     */
    fun onConsumeRemoveParticipantResult() {
        _state.update { it.copy(removeParticipantSuccess = null) }
    }

    /**
     * Method for processing when clicking on the call option
     *
     * @param userHandle Use handle
     * @param video Start call with video on or off
     * @param audio Start call with audio on or off
     */
    fun onCallTap(userHandle: Long, video: Boolean, audio: Boolean) =
        viewModelScope.launch {
            runCatching {
                get1On1ChatIdUseCase(userHandle)
            }.onSuccess { chatId ->
                startCall(chatId, video, audio)
            }.onFailure {
                Timber.e(it)
            }
        }

    /**
     * Starts a call
     *
     * @param chatId Chat id
     * @param video Start call with video on or off
     * @param audio Start call with audio on or off
     */
    private fun startCall(
        chatId: Long,
        video: Boolean,
        audio: Boolean,
    ) {
        if (chatApiGateway.getChatCall(chatId) != null) {
            Timber.d("There is a call, open it")
            CallUtil.openMeetingInProgress(
                MegaApplication.getInstance().applicationContext,
                chatId,
                true,
            )
            return
        }

        viewModelScope.launch {
            runCatching {
                setChatVideoInDeviceUseCase()
                startCallUseCase(chatId = chatId, audio = true, video = video)
            }.onFailure { exception ->
                Timber.e(exception)
            }.onSuccess { call ->
                call?.apply {
                    CallUtil.addChecksForACall(chatId, hasLocalVideo)
                    if (isOutgoing) {
                        chatManagement.setRequestSentCall(callId, isRequestSent = true)
                    }

                    openMeetingWithAudioOrVideo(
                        MegaApplication.getInstance().applicationContext,
                        chatId,
                        hasLocalAudio,
                        hasLocalVideo,
                    )
                }
            }
        }
    }

    /**
     * Monitor participants updates
     */
    private fun monitorChatParticipantsUpdates() {
        monitorChatParticipantsUpdatesJob?.cancel()
        monitorChatParticipantsUpdatesJob = viewModelScope.launch {
            monitorChatParticipantsUseCase(_state.value.chatId)
                .catch { Timber.e(it) }
                .collect {
                    it.forEach { participant ->
                        _state.update { it.copy(participantUpdated = participant) }
                    }
                }
        }
    }

    /**
     * End for all the current call
     */
    fun endCallForAll() = viewModelScope.launch {
        runCatching {
            endCallUseCase(_state.value.chatId)
            sendStatisticsMeetingsUseCase(EndCallForAll())
        }.onFailure {
            Timber.e(it.stackTraceToString())
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
     * Launch broadcast notifying that should leave a chat
     *
     * @param chatId [Long] ID of the chat to leave.
     */
    fun launchBroadcastLeaveChat(chatId: Long) = viewModelScope.launch {
        broadcastLeaveChatUseCase(chatId)
    }

    /**
     * monitor SFU Server Upgrade
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
        _state.update { it.copy(showForceUpdateDialog = true) }
    }

    /**
     * Set to false to hide the dialog
     */
    fun onForceUpdateDialogDismissed() {
        _state.update { it.copy(showForceUpdateDialog = false) }
    }

    /**
     * Get chat call updates
     */
    private fun monitorCallInChat() {
        monitorChatCallJob?.cancel()
        monitorChatCallJob = viewModelScope.launch {
            monitorCallInChatUseCase(_state.value.chatId)
                .catch {
                    Timber.e(it)
                }
                .collectLatest { call ->
                    _state.update { it.copy(call = call) }
                    call?.let { setShouldShowUserLimitsWarning(it) }
                    call?.changes?.apply {
                        if (contains(ChatCallChanges.Status)) {
                            Timber.d("Chat call status: ${call.status}")
                            when (call.status) {
                                ChatCallStatus.Destroyed -> {
                                    // Call has ended
                                    _state.update { it.copy(shouldShowUserLimitsWarning = false) }
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
                ?: FREE_PLAN_PARTICIPANTS_LIMIT
            val shouldShowWarning =
                (call.peerIdParticipants?.size
                    ?: 0) >= limit && _state.value.isCallUnlimitedProPlanFeatureFlagEnabled
            _state.update { it.copy(shouldShowUserLimitsWarning = shouldShowWarning) }
        } else {
            _state.update { it.copy(shouldShowUserLimitsWarning = false) }
        }
    }

    companion object {
        private const val MAX_LENGTH_CHAT_TITLE = 60
    }
}
