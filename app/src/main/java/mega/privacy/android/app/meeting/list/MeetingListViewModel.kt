package mega.privacy.android.app.meeting.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import mega.privacy.android.domain.usecase.ArchiveChat
import mega.privacy.android.domain.usecase.GetMeetings
import mega.privacy.android.domain.usecase.LeaveChat
import mega.privacy.android.domain.usecase.SignalChatPresenceActivity
import mega.privacy.android.domain.usecase.meeting.AnswerChatCall
import mega.privacy.android.domain.usecase.meeting.StartChatCallNoRinging
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
 * @property startChatCallNoRinging             [StartChatCallNoRinging]
 * @property answerChatCall                     [AnswerChatCall]
 * @property deviceGateway                      [DeviceGateway]
 * @property chatManagement                     [ChatManagement]
 * @property passcodeManagement                 [PasscodeManagement]
 * @property megaChatApiGateway                 [MegaChatApiGateway]
 * @property rtcAudioManagerGateway             [RTCAudioManagerGateway]
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class MeetingListViewModel @Inject constructor(
    private val archiveChatUseCase: ArchiveChat,
    private val leaveChatUseCase: LeaveChat,
    private val signalChatPresenceUseCase: SignalChatPresenceActivity,
    private val getMeetingsUseCase: GetMeetings,
    private val getLastMessageUseCase: GetLastMessageUseCase,
    private val meetingLastTimestampMapper: MeetingLastTimestampMapper,
    private val scheduledMeetingTimestampMapper: ScheduledMeetingTimestampMapper,
    private val startChatCallNoRinging: StartChatCallNoRinging,
    private val answerChatCall: AnswerChatCall,
    private val deviceGateway: DeviceGateway,
    private val chatManagement: ChatManagement,
    private val passcodeManagement: PasscodeManagement,
    private val megaChatApiGateway: MegaChatApiGateway,
    private val rtcAudioManagerGateway: RTCAudioManagerGateway,
) : ViewModel() {

    companion object {
        private const val DEBOUNCE_TIMEOUT_MS = 250L
    }

    private val state = MutableStateFlow(MeetingListState())
    private val searchQueryState = MutableStateFlow<String?>(null)
    private val is24HourFormat: Boolean by lazy { deviceGateway.is24HourFormat() }
    private val mutex = Mutex()

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
        getMeetingsUseCase(mutex)
            .debounce(DEBOUNCE_TIMEOUT_MS) // Needed for backpressure
            .mapLatest { items ->
                mutex.withLock {
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
     * Check if search query is empty
     *
     * @return  true if search query is empty, false otherwise
     */
    fun isSearchQueryEmpty(): Boolean =
        searchQueryState.value.isNullOrBlank()

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
    fun joinSchedMeeting(chatId: Long) =
        viewModelScope.launch {
            answerChatCall(
                chatId = chatId,
                video = false,
                audio = true
            )?.let { call ->
                call.chatId.takeIf { it != megaChatApiGateway.getChatInvalidHandle() }
                    ?.let { callChatId ->
                        chatManagement.removeJoiningCallChatId(chatId)
                        rtcAudioManagerGateway.removeRTCAudioManagerRingIn()
                        chatManagement.setSpeakerStatus(callChatId, call.hasLocalVideo)
                        chatManagement.setRequestSentCall(call.callId, true)
                        CallUtil.clearIncomingCallNotification(call.callId)
                        passcodeManagement.showPasscodeScreen = true
                        MegaApplication.getInstance().openCallService(callChatId)
                        state.update { it.copy(currentCallChatId = callChatId) }
                    }
            }
        }

    /**
     * Start scheduled meeting call
     *
     * @param chatId    Chat Id.
     * @param schedId   Scheduled meeting Id.
     */
    fun startSchedMeeting(chatId: Long, schedId: Long) =
        viewModelScope.launch {
            startChatCallNoRinging(
                chatId = chatId,
                schedId = schedId,
                enabledVideo = false,
                enabledAudio = true
            )?.let { call ->
                call.chatId.takeIf { it != megaChatApiGateway.getChatInvalidHandle() }
                    ?.let { callChatId ->
                        chatManagement.setSpeakerStatus(callChatId, false)
                        chatManagement.setRequestSentCall(call.callId, true)
                        passcodeManagement.showPasscodeScreen = true
                        MegaApplication.getInstance().openCallService(callChatId)
                        state.update { it.copy(currentCallChatId = callChatId) }
                    }
            }
        }

    /**
     * Remove current chat call
     */
    fun removeCurrentCall() {
        state.update { it.copy(currentCallChatId = null) }
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
}
