package mega.privacy.android.app.presentation.chat.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.R
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.chat.list.model.ChatsTabState
import mega.privacy.android.app.presentation.chat.mapper.ChatRoomTimestampMapper
import mega.privacy.android.app.presentation.data.SnackBarItem
import mega.privacy.android.app.usecase.chat.GetLastMessageUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.ChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItem.MeetingChatRoomItem
import mega.privacy.android.domain.entity.chat.ChatRoomItemStatus
import mega.privacy.android.domain.entity.chat.MeetingTooltipItem
import mega.privacy.android.domain.usecase.LeaveChat
import mega.privacy.android.domain.usecase.SignalChatPresenceActivity
import mega.privacy.android.domain.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.domain.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase
import mega.privacy.android.domain.usecase.chat.GetChatsUseCase.ChatRoomType
import mega.privacy.android.domain.usecase.chat.GetCurrentChatStatusUseCase
import mega.privacy.android.domain.usecase.chat.GetMeetingTooltipsUseCase
import mega.privacy.android.domain.usecase.chat.SetNextMeetingTooltipUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.IsParticipatingInChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.MonitorScheduledMeetingCanceledUseCase
import mega.privacy.android.domain.usecase.meeting.OpenOrStartCall
import mega.privacy.android.domain.usecase.meeting.StartChatCallNoRingingUseCase
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Chat tabs view model
 *
 * @property archiveChatUseCase
 * @property leaveChatUseCase
 * @property signalChatPresenceUseCase
 * @property getChatsUseCase
 * @property getLastMessageUseCase
 * @property chatRoomTimestampMapper
 * @property startChatCallNoRingingUseCase
 * @property openOrStartCall
 * @property answerChatCallUseCase
 * @property chatManagement
 * @property passcodeManagement
 * @property megaChatApiGateway
 * @property rtcAudioManagerGateway
 * @property getCurrentChatStatusUseCase
 * @property clearChatHistoryUseCase
 * @property isParticipatingInChatCallUseCase
 * @property getMeetingTooltipsUseCase
 * @property setNextMeetingTooltipUseCase
 * @property getFeatureFlagValue
 * @property monitorScheduledMeetingCanceledUseCase
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatTabsViewModel @Inject constructor(
    private val archiveChatUseCase: ArchiveChatUseCase,
    private val leaveChatUseCase: LeaveChat,
    private val signalChatPresenceUseCase: SignalChatPresenceActivity,
    private val getChatsUseCase: GetChatsUseCase,
    private val getLastMessageUseCase: GetLastMessageUseCase,
    private val chatRoomTimestampMapper: ChatRoomTimestampMapper,
    private val startChatCallNoRingingUseCase: StartChatCallNoRingingUseCase,
    private val openOrStartCall: OpenOrStartCall,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val chatManagement: ChatManagement,
    private val passcodeManagement: PasscodeManagement,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
    private val getCurrentChatStatusUseCase: GetCurrentChatStatusUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
    private val isParticipatingInChatCallUseCase: IsParticipatingInChatCallUseCase,
    private val getMeetingTooltipsUseCase: GetMeetingTooltipsUseCase,
    private val setNextMeetingTooltipUseCase: SetNextMeetingTooltipUseCase,
    private val getFeatureFlagValue: GetFeatureFlagValueUseCase,
    private val monitorScheduledMeetingCanceledUseCase: MonitorScheduledMeetingCanceledUseCase,
) : ViewModel() {

    private val state = MutableStateFlow(ChatsTabState())
    private var meetingsRequested = false

    /**
     * Get view model state
     */
    fun getState(): StateFlow<ChatsTabState> = state

    init {
        signalChatPresence()
        requestChats()
        retrieveChatStatus()
        retrieveTooltips()

        viewModelScope.launch {
            monitorScheduledMeetingCanceledUseCase().conflate()
                .collect { messageResId -> triggerSnackbarMessage(messageResId) }
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
                        it.copy(chats = items)
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
                            it.copy(meetings = items)
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
    fun startMeetingCall(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                state.value.findChatItem(chatId)
                    ?.takeIf { it is MeetingChatRoomItem }
                    ?.let { item ->
                        val meeting = item as MeetingChatRoomItem
                        when (meeting.currentCallStatus) {
                            is ChatRoomItemStatus.NotJoined ->
                                answerChatCallUseCase(chatId = chatId, video = false, audio = true)
                                    ?.takeIf { it.chatId != megaChatApiGateway.getChatInvalidHandle() }
                                    ?.let { call ->
                                        chatManagement.removeJoiningCallChatId(chatId)
                                        rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                                        CallUtil.clearIncomingCallNotification(call.callId)
                                        openCurrentCall(call)
                                    }

                            is ChatRoomItemStatus.NotStarted ->
                                if (meeting.schedId != null) {
                                    startChatCallNoRingingUseCase(
                                        chatId = chatId,
                                        schedId = meeting.schedId!!,
                                        enabledVideo = false,
                                        enabledAudio = true
                                    )?.takeIf { it.chatId != megaChatApiGateway.getChatInvalidHandle() }
                                        ?.let(::openCurrentCall)
                                } else {
                                    openOrStartCall(
                                        chatId = chatId,
                                        video = false,
                                        audio = true
                                    )?.takeIf { it.chatId != megaChatApiGateway.getChatInvalidHandle() }
                                        ?.let(::openCurrentCall)
                                }

                            else -> {
                                // Nothing to do
                            }
                        }
                    }
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Open current call
     *
     * @param call  [ChatCall]
     */
    private fun openCurrentCall(call: ChatCall) {
        chatManagement.setSpeakerStatus(call.chatId, false)
        chatManagement.setRequestSentCall(call.callId, call.isOutgoing)
        passcodeManagement.showPasscodeScreen = true
        MegaApplication.getInstance().openCallService(call.chatId)
        state.update { it.copy(currentCallChatId = call.chatId) }
    }

    /**
     * Remove current chat call
     */
    fun removeCurrentCall() {
        state.update { it.copy(currentCallChatId = null) }
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
                stringParam = state.value.findChatItem(chatIds.first())?.title
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
    fun leaveChat(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                leaveChatUseCase(chatId)
            }.onFailure { exception ->
                Timber.e(exception)
            }
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
     */
    private fun retrieveTooltips() {
        viewModelScope.launch {
            val featureFlagEnabled = getFeatureFlagValue(AppFeatures.ScheduleMeeting)
            if (featureFlagEnabled) {
                runCatching {
                    delay(TimeUnit.SECONDS.toMillis(2)) // Delay required by design
                    getMeetingTooltipsUseCase()
                }.onSuccess { tooltips ->
                    state.update { it.copy(tooltip = tooltips) }
                }.onFailure(Timber.Forest::e)
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
}
