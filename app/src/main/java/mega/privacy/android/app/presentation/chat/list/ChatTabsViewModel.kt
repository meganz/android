package mega.privacy.android.app.presentation.chat.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.presentation.chat.list.model.ChatTab
import mega.privacy.android.app.presentation.chat.list.model.ChatsTabState
import mega.privacy.android.app.presentation.chat.mapper.ChatRoomTimestampMapper
import mega.privacy.android.app.presentation.data.SnackBarItem
import mega.privacy.android.app.presentation.mapper.GetStringFromStringResMapper
import mega.privacy.android.app.usecase.chat.GetLastMessageUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.call.CallNotificationType
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.ChatCallChanges
import mega.privacy.android.domain.entity.call.ChatCallStatus
import mega.privacy.android.domain.entity.call.ChatCallTermCodeType
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.MeetingChatRoomItem
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import mega.privacy.android.domain.entity.meeting.ChatRoomItemStatus
import mega.privacy.android.domain.exception.MegaException
import mega.privacy.android.domain.usecase.SignalChatPresenceActivity
import mega.privacy.android.domain.usecase.call.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.call.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.call.OpenOrStartCallUseCase
import mega.privacy.android.domain.usecase.call.StartChatCallNoRingingUseCase
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUnreadStatusUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase.ChatRoomType
import mega.privacy.android.domain.usecase.chat.GetCurrentChatStatusUseCase
import mega.privacy.android.domain.usecase.chat.GetMeetingTooltipsUseCase
import mega.privacy.android.domain.usecase.chat.HasArchivedChatsUseCase
import mega.privacy.android.domain.usecase.chat.LeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.MonitorLeaveChatUseCase
import mega.privacy.android.domain.usecase.chat.SetNextMeetingTooltipUseCase
import mega.privacy.android.domain.usecase.contact.MonitorHasAnyContactUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorChatCallUpdatesUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingCanceledUseCase
import mega.privacy.android.domain.usecase.meeting.StartMeetingInWaitingRoomChatUseCase
import mega.privacy.android.shared.resources.R as sharedR
import nz.mega.sdk.MegaChatError
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Chat tabs view model
 *
 * @property archiveChatUseCase                         [ArchiveChatUseCase]
 * @property leaveChatUseCase                           [LeaveChatUseCase]
 * @property signalChatPresenceUseCase                  [SignalChatPresenceActivity]
 * @property getChatsUseCase                            [GetChatsUseCase]
 * @property getLastMessageUseCase                      [GetLastMessageUseCase]
 * @property chatRoomTimestampMapper                    [ChatRoomTimestampMapper]
 * @property startChatCallNoRingingUseCase              [StartChatCallNoRingingUseCase]
 * @property openOrStartCallUseCase                     [OpenOrStartCallUseCase]
 * @property answerChatCallUseCase                      [AnswerChatCallUseCase]
 * @property chatManagement                             [ChatManagement]
 * @property megaChatApiGateway                         [MegaChatApiGateway]
 * @property rtcAudioManagerGateway                     [RTCAudioManagerGateway]
 * @property getCurrentChatStatusUseCase                [GetCurrentChatStatusUseCase]
 * @property clearChatHistoryUseCase                    [ClearChatHistoryUseCase]
 * @property isParticipatingInChatCallUseCase           [IsParticipatingInChatCallUseCase]
 * @property getMeetingTooltipsUseCase                  [GetMeetingTooltipsUseCase]
 * @property setNextMeetingTooltipUseCase               [SetNextMeetingTooltipUseCase]
 * @property monitorScheduledMeetingCanceledUseCase     [MonitorScheduledMeetingCanceledUseCase]
 * @property getChatsUnreadStatusUseCase                [GetChatsUnreadStatusUseCase]
 * @property startMeetingInWaitingRoomChatUseCase       [StartMeetingInWaitingRoomChatUseCase]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatTabsViewModel @Inject constructor(
    private val archiveChatUseCase: ArchiveChatUseCase,
    private val leaveChatUseCase: LeaveChatUseCase,
    private val signalChatPresenceUseCase: SignalChatPresenceActivity,
    private val getChatsUseCase: GetChatsUseCase,
    private val getLastMessageUseCase: GetLastMessageUseCase,
    private val chatRoomTimestampMapper: ChatRoomTimestampMapper,
    private val startChatCallNoRingingUseCase: StartChatCallNoRingingUseCase,
    private val openOrStartCallUseCase: OpenOrStartCallUseCase,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val chatManagement: ChatManagement,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val getCurrentChatStatusUseCase: GetCurrentChatStatusUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase,
    private val getMeetingTooltipsUseCase: GetMeetingTooltipsUseCase,
    private val setNextMeetingTooltipUseCase: SetNextMeetingTooltipUseCase,
    private val monitorScheduledMeetingCanceledUseCase: MonitorScheduledMeetingCanceledUseCase,
    private val getChatsUnreadStatusUseCase: GetChatsUnreadStatusUseCase,
    private val startMeetingInWaitingRoomChatUseCase: StartMeetingInWaitingRoomChatUseCase,
    private val monitorLeaveChatUseCase: MonitorLeaveChatUseCase,
    private val monitorChatCallUpdatesUseCase: MonitorChatCallUpdatesUseCase,
    private val hasArchivedChatsUseCase: HasArchivedChatsUseCase,
    private val monitorHasAnyContactUseCase: MonitorHasAnyContactUseCase,
    private val getStringFromStringResMapper: GetStringFromStringResMapper,
) : ViewModel() {

    private val state = MutableStateFlow(ChatsTabState())
    private var meetingsRequested = false

    private var monitorChatCallUpdatesJob: Job? = null

    /**
     * Get view model state
     */
    fun getState(): StateFlow<ChatsTabState> = state

    init {
        signalChatPresence()
        requestChats()
        retrieveChatStatus()
        retrieveTooltips()
        retrieveChatsUnreadStatus()
        monitorLeaveChat()
        monitorHasAnyContact()
        viewModelScope.launch {
            monitorScheduledMeetingCanceledUseCase().conflate()
                .collect { messageResId -> triggerSnackbarMessage(messageResId) }
        }
    }

    private fun monitorHasAnyContact() {
        viewModelScope.launch {
            monitorHasAnyContactUseCase().conflate()
                .collect { hasAnyContact ->
                    state.update { state -> state.copy(hasAnyContact = hasAnyContact) }
                }
        }
    }

    /**
     * Check has archived chats
     *
     */
    fun checkHasArchivedChats() {
        viewModelScope.launch {
            runCatching { hasArchivedChatsUseCase() }
                .onSuccess { hasArchivedChats -> state.update { it.copy(hasArchivedChats = hasArchivedChats) } }
                .onFailure(Timber::e)
        }
    }

    private fun monitorLeaveChat() {
        viewModelScope.launch {
            monitorLeaveChatUseCase()
                .catch {
                    Timber.e(it)
                }.collect { chatId ->
                    if (chatId != -1L) {
                        performLeaveChat(chatId)
                    }
                }
        }
    }

    /**
     * Leave a chat
     *
     * @param chatId    [Long] ID of the chat to leave.
     */
    private suspend fun performLeaveChat(chatId: Long) {
        runCatching {
            chatManagement.addLeavingChatId(chatId)
            leaveChatUseCase(chatId)
        }.onFailure { exception ->
            Timber.e("Leaving chat $exception")
            chatManagement.removeLeavingChatId(chatId)
        }.onSuccess {
            chatManagement.removeLeavingChatId(chatId)
        }
    }

    /**
     * Request Chat Rooms
     */
    private fun requestChats() {
        viewModelScope.launch {
            getChatsUseCase(
                chatRoomType = ChatRoomType.NON_MEETINGS,
                lastMessage = getLastMessageUseCase::invoke,
                lastTimeMapper = chatRoomTimestampMapper::getLastTimeFormatted,
                meetingTimeMapper = chatRoomTimestampMapper::getMeetingTimeFormatted,
                headerTimeMapper = chatRoomTimestampMapper::getHeaderTimeFormatted,
            )
                .conflate()
                .catch { Timber.e(it) }
                .collect { items ->
                    state.update {
                        it.copy(
                            areChatsLoading = false,
                            chats = items,
                            onlyNoteToSelfChat = items.size == 1 && items.first() is ChatRoomItem.NoteToSelfChatRoomItem,
                            areChatsOrMeetingLoading = if (it.currentTab == ChatTab.CHATS) false else it.areChatsOrMeetingLoading,
                            isEmptyChatsOrMeetings = if (it.currentTab == ChatTab.CHATS) it.noChats else it.noMeetings
                        )
                    }
                }
        }
    }

    /**
     * Request Meeting Rooms
     */
    fun requestMeetings() {
        if (!meetingsRequested) {
            meetingsRequested = true
            viewModelScope.launch {
                getChatsUseCase(
                    chatRoomType = ChatRoomType.MEETINGS,
                    lastMessage = getLastMessageUseCase::invoke,
                    lastTimeMapper = chatRoomTimestampMapper::getLastTimeFormatted,
                    meetingTimeMapper = chatRoomTimestampMapper::getMeetingTimeFormatted,
                    headerTimeMapper = chatRoomTimestampMapper::getHeaderTimeFormatted,
                )
                    .conflate()
                    .catch { Timber.e(it) }
                    .collect { items ->
                        state.update {
                            it.copy(
                                areMeetingsLoading = false,
                                meetings = items,
                                areChatsOrMeetingLoading = if (it.currentTab == ChatTab.MEETINGS) false else it.areChatsOrMeetingLoading,
                                isEmptyChatsOrMeetings = if (it.currentTab == ChatTab.MEETINGS) it.noMeetings else it.noChats
                            )
                        }
                    }
            }
        }
    }

    /**
     * Get chat item updates given a Chat Id
     *
     * @param chatId
     */
    fun getChatRoom(chatId: Long): Flow<ChatRoomItem?> =
        state.mapLatest { it.findChatItem(chatId) }.also { signalChatPresence() }

    /**
     * Get chat item
     *
     * @param chatId
     */
    fun getChatItem(chatId: Long): ChatRoomItem? =
        state.value.findChatItem(chatId)

    private fun ChatsTabState.findChatItem(chatId: Long): ChatRoomItem? =
        chats.firstOrNull { chat -> chat.chatId == chatId }
            ?: meetings.firstOrNull { meeting -> meeting.chatId == chatId }

    /**
     * Start meeting call
     *
     * @param chatId
     */
    fun startMeetingCall(chatId: Long, enablePasscode: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                state.value.findChatItem(chatId)
                    ?.takeIf { it is MeetingChatRoomItem }
                    ?.let { item ->
                        val meeting = item as MeetingChatRoomItem
                        when (meeting.currentCallStatus) {
                            ChatRoomItemStatus.NotJoined -> {
                                if (meeting.isWaitingRoom && !meeting.hasPermissions) {
                                    state.update { it.copy(currentWaitingRoom = chatId) }
                                } else {
                                    answerChatCallUseCase(
                                        chatId = chatId,
                                        video = false,
                                        audio = true
                                    )?.takeIf { it.chatId != megaChatApiGateway.getChatInvalidHandle() }
                                        ?.let { call ->
                                            chatManagement.removeJoiningCallChatId(chatId)
                                            rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                                            CallUtil.clearIncomingCallNotification(call.callId)
                                            openCurrentCall(call, enablePasscode)
                                        }
                                }
                            }

                            ChatRoomItemStatus.NotStarted -> {
                                if (meeting.isWaitingRoom && !meeting.hasPermissions) {
                                    state.update { it.copy(currentWaitingRoom = chatId) }
                                } else {
                                    if (meeting.schedId != null) {
                                        if (meeting.isWaitingRoom) {
                                            startMeetingInWaitingRoomChatUseCase(
                                                chatId = chatId,
                                                schedIdWr = meeting.schedId!!,
                                                enabledVideo = false,
                                                enabledAudio = true
                                            )?.takeIf { it.chatId != megaChatApiGateway.getChatInvalidHandle() }
                                                ?.let { openCurrentCall(it, enablePasscode) }
                                        } else {
                                            startChatCallNoRingingUseCase(
                                                chatId = chatId,
                                                schedId = meeting.schedId!!,
                                                enabledVideo = false,
                                                enabledAudio = true
                                            )?.takeIf { it.chatId != megaChatApiGateway.getChatInvalidHandle() }
                                                ?.let { openCurrentCall(it, enablePasscode) }
                                        }
                                    } else {
                                        runCatching {
                                            openOrStartCallUseCase(
                                                chatId = chatId,
                                                audio = true,
                                                video = false
                                            )
                                        }.onSuccess { call ->
                                            call?.let { openCurrentCall(it, enablePasscode) }
                                        }.onFailure {
                                            Timber.e("Exception opening or starting call: $it")
                                        }
                                    }
                                }
                            }

                            else -> {
                                // Nothing to do
                            }
                        }
                    }
            }.onFailure { error ->
                if (error is MegaException && error.errorCode == MegaChatError.ERROR_ACCESS) {
                    state.update { it.copy(currentWaitingRoom = chatId) }
                } else {
                    Timber.e(error)
                }
            }
        }
    }

    /**
     * Get chat call updates
     */
    private fun getChatCallUpdates(chatId: Long) {
        monitorChatCallUpdatesJob?.cancel()
        monitorChatCallUpdatesJob = viewModelScope.launch {
            monitorChatCallUpdatesUseCase()
                .filter { it.chatId == chatId }
                .catch {
                    Timber.e(it)
                }
                .collect { call ->
                    call.changes?.apply {
                        Timber.d("Monitor chat call updated, changes ${call.changes}")
                        if (contains(ChatCallChanges.Status)) {
                            call.status?.let {
                                Timber.d("Monitor chat call updated, status $it, and call term code is ${call.termCode}")
                                if (it == ChatCallStatus.TerminatingUserParticipation && call.termCode == ChatCallTermCodeType.ProtocolVersion) {
                                    showForceUpdateDialog()
                                }
                            }
                        } else if (contains(ChatCallChanges.GenericNotification)) {
                            if (call.notificationType == CallNotificationType.SFUError && call.termCode == ChatCallTermCodeType.ProtocolVersion) {
                                showForceUpdateDialog()
                            }
                        }
                    }
                }
        }
    }

    private fun showForceUpdateDialog() {
        state.update { it.copy(showForceUpdateDialog = true) }
    }

    /**
     * Set to false to hide the dialog
     */
    fun onForceUpdateDialogDismissed() {
        state.update { it.copy(showForceUpdateDialog = false) }
    }

    /**
     * Open current call
     *
     * @param call  [ChatCall]
     */
    private fun openCurrentCall(call: ChatCall, enablePasscode: () -> Unit) {
        chatManagement.setSpeakerStatus(call.chatId, false)
        chatManagement.setRequestSentCall(call.callId, call.isOutgoing)
        enablePasscode()
        MegaApplication.getInstance().openCallService(call.chatId)
        getChatCallUpdates(call.chatId)
        state.update { it.copy(currentCallChatId = call.chatId) }
    }

    /**
     * Remove current chat call and Waiting Room
     */
    fun removeCurrentCallAndWaitingRoom() {
        state.update {
            it.copy(
                currentCallChatId = null,
                currentWaitingRoom = null,
            )
        }
    }

    /**
     * Update snackBar
     */
    fun updateSnackBar(snackBarItem: SnackBarItem?) {
        state.update { it.copy(snackBar = snackBarItem) }
    }

    /**
     * Archive chats
     *
     * @param chatIds    Chat ids to be archived
     */
    fun archiveChats(vararg chatIds: Long) {
        val singleParam = chatIds.size == 1
        viewModelScope.launch {
            var stringParam: String? = null
            var intParam: Int? = null
            if (singleParam) {
                stringParam = state.value.findChatItem(chatIds.first())?.let {
                    if (it is ChatRoomItem.NoteToSelfChatRoomItem) getStringFromStringResMapper(
                        sharedR.string.chat_note_to_self_chat_title
                    ) else it.title
                }
            } else {
                intParam = chatIds.size
            }

            runCatching { chatIds.forEach { archiveChatUseCase(it, true) } }
                .onSuccess {
                    updateSnackBar(
                        SnackBarItem(
                            stringRes = if (singleParam)
                                R.string.success_archive_chat
                            else
                                R.string.archived_chats_show_option,
                            stringArg = stringParam,
                            intArg = intParam,
                        )
                    )
                    checkHasArchivedChats()
                }
                .onFailure {
                    Timber.e(it)
                    if (singleParam) {
                        updateSnackBar(
                            SnackBarItem(
                                stringRes = R.string.error_archive_chat,
                                stringArg = stringParam
                            )
                        )
                    }
                }
        }
    }

    /**
     * Leave chat
     *
     * @param chatId    Chat id to leave
     */
    fun leaveChat(chatId: Long) =
        viewModelScope.launch {
            runCatching {
                chatManagement.addLeavingChatId(chatId)
                leaveChatUseCase(chatId)
            }.onFailure { exception ->
                chatManagement.removeLeavingChatId(chatId)
                Timber.e(exception)
            }.onSuccess {
                Timber.d("Chat left ")
                chatManagement.removeLeavingChatId(chatId)
            }
        }

    /**
     * Leave chats
     *
     * @param chatIds   Chat ids to leave
     */
    fun leaveChats(chatIds: List<Long>) {
        chatIds.forEach(::leaveChat)
    }

    /**
     * Signal chat presence
     */
    fun signalChatPresence() {
        viewModelScope.launch {
            runCatching {
                signalChatPresenceUseCase()
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Cancel Call Update Job
     */
    fun cancelCallUpdate() {
        monitorChatCallUpdatesJob?.cancel()
    }

    /**
     * Retrieve current chat status
     */
    private fun retrieveChatStatus() {
        viewModelScope.launch {
            getCurrentChatStatusUseCase()
                .catch { Timber.e(it) }
                .collectLatest { currentStatus ->
                    state.update {
                        it.copy(currentChatStatus = currentStatus)
                    }
                }
        }
    }

    /**
     * Retrieve Scheduled Meeting Tooltips to be shown
     *
     * @param delayShowing  Flag to delay the showing
     */
    private fun retrieveTooltips(delayShowing: Boolean = true) {
        viewModelScope.launch {
            runCatching {
                if (delayShowing) {
                    delay(TimeUnit.SECONDS.toMillis(2)) // Delay required by design
                }
                getMeetingTooltipsUseCase()
            }.onSuccess { tooltips ->
                state.update { it.copy(tooltip = tooltips) }
            }.onFailure(Timber.Forest::e)
        }
    }

    /**
     * Retrieve unread status for Chat and Meeting tabs
     */
    private fun retrieveChatsUnreadStatus() {
        viewModelScope.launch {
            getChatsUnreadStatusUseCase()
                .catch { Timber.e(it) }
                .collectLatest { result ->
                    state.update { it.copy(currentUnreadStatus = result) }
                }
        }
    }

    /**
     * On item selected
     *
     * @param chatId    Chat selected
     */
    fun onItemSelected(chatId: Long) {
        state.update { oldState ->
            val selectedMeetings = oldState.selectedIds.toMutableList()
            if (selectedMeetings.contains(chatId)) {
                selectedMeetings.remove(chatId)
            } else {
                selectedMeetings.add(chatId)
            }

            oldState.copy(selectedIds = selectedMeetings)
        }
    }

    /**
     * On items selected
     *
     * @param chatIds   Chats selected
     */
    fun onItemsSelected(chatIds: List<Long>) {
        state.update { it.copy(selectedIds = chatIds) }
    }

    /**
     * Clear meetings selection
     */
    fun clearSelection() {
        state.update { it.copy(selectedIds = emptyList()) }
    }

    /**
     * Set tab selected
     *
     * @param selectedTab   [ChatTab]
     */
    fun setTabSelected(selectedTab: ChatTab) {
        state.update {
            it.copy(
                currentTab = selectedTab,
                areChatsOrMeetingLoading = if (selectedTab == ChatTab.CHATS) it.areChatsLoading else it.areMeetingsLoading,
                isEmptyChatsOrMeetings = if (selectedTab == ChatTab.CHATS) it.noChats else it.noMeetings
            )
        }
    }

    /**
     * Check if meeting tab is shown
     */
    fun isMeetingTabShown(): Boolean = state.value.currentTab == ChatTab.MEETINGS


    /**
     * Set chat search query
     *
     * @param query     Search text
     */
    fun setSearchQuery(query: String?) {
        state.update { it.copy(searchQuery = query) }.also { signalChatPresence() }
    }

    /**
     * Clear search query
     */
    fun clearSearchQuery() {
        setSearchQuery(null)
    }

    /**
     * Clear chat history
     *
     * @param chatId    Chat id
     */
    fun clearChatHistory(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                clearChatHistoryUseCase(chatId)
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Check participating in chat call
     */
    fun checkParticipatingInChatCall() {
        viewModelScope.launch {
            runCatching {
                isParticipatingInChatCallUseCase()
            }.onSuccess { isInCall ->
                state.update { it.copy(isParticipatingInChatCallResult = isInCall) }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    /**
     * Mark handle is participating in chat call
     */
    fun markHandleIsParticipatingInChatCall() {
        state.update { it.copy(isParticipatingInChatCallResult = null) }
    }

    /**
     * Set next meeting tooltip to be shown
     *
     * @param tooltip   [MeetingTooltipItem] to be shown next
     */
    fun setNextMeetingTooltip(tooltip: MeetingTooltipItem) {
        viewModelScope.launch {
            runCatching {
                setNextMeetingTooltipUseCase(tooltip)
            }.onSuccess {
                retrieveTooltips()
            }.onFailure(Timber.Forest::e)
        }
    }

    /**
     * Trigger event to show Snackbar message
     *
     * @param messageResId  Content for snack bar
     */
    private fun triggerSnackbarMessage(messageResId: Int) =
        state.update { it.copy(snackbarMessageContent = triggered(messageResId)) }

    /**
     * Reset and notify that snackbarMessage is consumed
     */
    fun onSnackbarMessageConsumed() = state.update { it.copy(snackbarMessageContent = consumed()) }

    /**
     * Temporary show or hide tooltips
     *
     * @param show  True to show tooltips, false otherwise
     */
    fun showTooltips(show: Boolean) {
        if (show) {
            retrieveTooltips(false)
        } else {
            state.update { it.copy(tooltip = MeetingTooltipItem.NONE) }
        }
    }
}
