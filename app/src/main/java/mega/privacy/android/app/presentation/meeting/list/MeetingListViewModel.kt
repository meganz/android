package mega.privacy.android.app.presentation.meeting.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.components.ChatManagement
import mega.privacy.android.app.meeting.gateway.RTCAudioManagerGateway
import mega.privacy.android.app.objects.PasscodeManagement
import mega.privacy.android.app.presentation.meeting.mapper.MeetingLastTimestampMapper
import mega.privacy.android.app.presentation.meeting.mapper.ScheduledMeetingTimestampMapper
import mega.privacy.android.app.presentation.meeting.model.MeetingListState
import mega.privacy.android.app.usecase.meeting.GetLastMessageUseCase
import mega.privacy.android.app.utils.CallUtil
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.data.gateway.api.MegaChatApiGateway
import mega.privacy.android.domain.entity.chat.ChatCall
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import mega.privacy.android.domain.usecase.ArchiveChat
import mega.privacy.android.domain.usecase.GetMeetings
import mega.privacy.android.domain.usecase.LeaveChat
import mega.privacy.android.domain.usecase.SignalChatPresenceActivity
import mega.privacy.android.domain.usecase.meeting.AnswerChatCallUseCase
import mega.privacy.android.domain.usecase.meeting.OpenOrStartCall
import mega.privacy.android.domain.usecase.meeting.StartChatCallNoRingingUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Meeting list view model
 *
 * @property archiveChatUseCase                 [ArchiveChat]
 * @property leaveChatUseCase                   [LeaveChat]
 * @property signalChatPresenceUseCase          [SignalChatPresenceActivity]
 * @property getMeetingsUseCase                 [GetMeetings]
 * @property getLastMessageUseCase              [GetLastMessageUseCase]
 * @property meetingLastTimestampMapper         [MeetingLastTimestampMapper]
 * @property scheduledMeetingTimestampMapper    [ScheduledMeetingTimestampMapper]
 * @property startChatCallNoRingingUseCase      [StartChatCallNoRingingUseCase]
 * @property openOrStartCall                    [OpenOrStartCall]
 * @property answerChatCallUseCase              [AnswerChatCallUseCase]
 * @property deviceGateway                      [DeviceGateway]
 * @property chatManagement                     [ChatManagement]
 * @property passcodeManagement                 [PasscodeManagement]
 * @property megaChatApiGateway                 [MegaChatApiGateway]
 * @property rtcAudioManagerGateway             [RTCAudioManagerGateway]
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MeetingListViewModel @Inject constructor(
    private val archiveChatUseCase: ArchiveChat,
    private val leaveChatUseCase: LeaveChat,
    private val signalChatPresenceUseCase: SignalChatPresenceActivity,
    private val getMeetingsUseCase: GetMeetings,
    private val getLastMessageUseCase: GetLastMessageUseCase,
    private val meetingLastTimestampMapper: MeetingLastTimestampMapper,
    private val scheduledMeetingTimestampMapper: ScheduledMeetingTimestampMapper,
    private val startChatCallNoRingingUseCase: StartChatCallNoRingingUseCase,
    private val openOrStartCall: OpenOrStartCall,
    private val answerChatCallUseCase: AnswerChatCallUseCase,
    private val deviceGateway: DeviceGateway,
    private val chatManagement: ChatManagement,
    private val passcodeManagement: PasscodeManagement,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
) : ViewModel() {

    private val state = MutableStateFlow(MeetingListState())
    private val searchQueryState = MutableStateFlow<String?>(null)
    private val is24HourFormat: Boolean by lazy { deviceGateway.is24HourFormat() }

    /**
     * Get Meeting List state
     *
     * @return  MeetingListState
     */
    fun getState(): StateFlow<MeetingListState> = state

    init {
        viewModelScope.launch {
            combine(searchQueryState, getMeetings(), ::mapToFilteredMeetings)
                .catch { Timber.e(it) }
                .collectLatest { lastMeetings ->
                    state.update {
                        it.copy(meetings = lastMeetings)
                    }
                }
        }

        signalChatPresence()
    }

    /**
     * Get meetings
     *
     * @return  Flow of MeetingRoomItems
     */
    private fun getMeetings(): Flow<List<MeetingRoomItem>> =
        getMeetingsUseCase()
            .mapLatest { items ->
                withContext(Dispatchers.Default) {
                    items.map { item ->
                        item.copy(
                            lastMessage = getLastMessageUseCase.get(item.chatId)
                                .blockingGetOrNull(),
                            lastTimestampFormatted = meetingLastTimestampMapper
                                (item.lastTimestamp, is24HourFormat),
                            scheduledTimestampFormatted = scheduledMeetingTimestampMapper
                                (item, is24HourFormat)
                        )
                    }
                }
            }

    private fun mapToFilteredMeetings(
        searchQuery: String?,
        meetings: List<MeetingRoomItem>,
    ): List<MeetingRoomItem> =
        if (searchQuery.isNullOrBlank()) {
            meetings
        } else {
            meetings.filter { meeting ->
                meeting.title.contains(searchQuery, true) ||
                        meeting.lastMessage?.contains(searchQuery, true) == true
            }
        }

    /**
     * Get specific meeting given its chat id
     *
     * @param chatId    Chat id to identify chat
     * @return          LiveData with MeetingRoomItem
     */
    fun getMeeting(chatId: Long): Flow<MeetingRoomItem?> =
        state.mapLatest { it.meetings.firstOrNull { item -> item.chatId == chatId } }
            .also { signalChatPresence() }

    /**
     * Set search query string
     *
     * @param query Search query
     */
    fun setSearchQuery(query: String?) {
        searchQueryState.update { query }
        signalChatPresence()
    }

    /**
     * Join scheduled meeting call
     *
     * @param chatId    Chat Id.
     */
    fun joinSchedMeeting(chatId: Long) {
        viewModelScope.launch {
            Timber.d("Answer meeting")
            answerChatCallUseCase(
                chatId = chatId,
                video = false,
                audio = true
            )?.let { call ->
                call.chatId.takeIf { it != megaChatApiGateway.getChatInvalidHandle() }
                    ?.let {
                        chatManagement.removeJoiningCallChatId(chatId)
                        rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                        CallUtil.clearIncomingCallNotification(call.callId)
                        openCurrentCall(call)
                    }
            }
        }
    }

    /**
     * Start scheduled meeting call
     *
     * @param chatId    Chat Id.
     * @param schedId   Scheduled meeting Id.
     */
    fun startSchedMeeting(chatId: Long, schedId: Long?) {
        viewModelScope.launch {
            if (schedId == null || schedId == megaChatApiGateway.getChatInvalidHandle()) {
                Timber.d("Start meeting")
                openOrStartCall(
                    chatId = chatId,
                    video = false,
                    audio = true
                )?.let { call ->
                    call.chatId.takeIf { it != megaChatApiGateway.getChatInvalidHandle() }
                        ?.let {
                            openCurrentCall(call)
                        }
                }
            } else {
                Timber.d("Start scheduled meeting")
                startChatCallNoRingingUseCase(
                    chatId = chatId,
                    schedId = schedId,
                    enabledVideo = false,
                    enabledAudio = true
                )?.let { call ->
                    call.chatId.takeIf { it != megaChatApiGateway.getChatInvalidHandle() }
                        ?.let {
                            openCurrentCall(call)
                        }
                }
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
    fun updateSnackBar(text: Int?) {
        state.update { it.copy(snackBar = text) }
    }

    /**
     * Archive chat
     *
     * @param chatId    Chat id to be archived
     */
    fun archiveChat(chatId: Long) {
        viewModelScope.launch {
            runCatching {
                archiveChatUseCase(chatId, true)
            }.onFailure { exception ->
                Timber.e(exception)
            }
        }
    }

    /**
     * Archive chats
     *
     * @param chatIds   Chat ids to be archived
     */
    fun archiveChats(chatIds: List<Long>) {
        chatIds.forEach(::archiveChat)
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
     * Scroll to top
     */
    fun scrollToTop() {
        state.update { it.copy(scrollToTop = !it.scrollToTop) }
    }

    /**
     * On item selected
     *
     * @param chatId    Chat selected
     */
    fun onItemSelected(chatId: Long) {
        state.update { oldState ->
            val selectedMeetings = oldState.selectedMeetings.toMutableList()
            if (selectedMeetings.contains(chatId)) {
                oldState.copy(
                    selectedMeetings = selectedMeetings.apply { remove(chatId) }
                )
            } else {
                oldState.copy(
                    selectedMeetings = selectedMeetings.apply { add(chatId) }
                )
            }
        }
    }

    /**
     * On items selected
     *
     * @param chatIds   Chats selected
     */
    fun onItemsSelected(chatIds: List<Long>) {
        state.update { it.copy(selectedMeetings = chatIds) }
    }

    /**
     * Clear meetings selection
     */
    fun clearSelection() {
        state.update { it.copy(selectedMeetings = emptyList()) }
    }
}
