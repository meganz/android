package mega.privacy.android.app.meeting.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.presentation.meeting.mapper.MeetingLastTimestampMapper
import mega.privacy.android.app.presentation.meeting.mapper.ScheduledMeetingTimestampMapper
import mega.privacy.android.app.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.app.usecase.chat.LeaveChatUseCase
import mega.privacy.android.app.usecase.chat.SignalChatPresenceUseCase
import mega.privacy.android.app.usecase.meeting.GetLastMessageUseCase
import mega.privacy.android.app.utils.RxUtil.blockingGetOrNull
import mega.privacy.android.app.utils.notifyObserver
import mega.privacy.android.data.gateway.DeviceGateway
import mega.privacy.android.domain.entity.chat.MeetingRoomItem
import mega.privacy.android.domain.qualifier.DefaultDispatcher
import mega.privacy.android.domain.usecase.GetMeetings
import timber.log.Timber
import javax.inject.Inject

/**
 * Meeting list view model
 *
 * @property archiveChatUseCase
 * @property leaveChatUseCase
 * @property signalChatPresenceUseCase
 * @property getMeetingsUseCase
 * @property getLastMessageUseCase
 * @property dispatcher
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MeetingListViewModel @Inject constructor(
    private val archiveChatUseCase: ArchiveChatUseCase,
    private val leaveChatUseCase: LeaveChatUseCase,
    private val signalChatPresenceUseCase: SignalChatPresenceUseCase,
    private val getMeetingsUseCase: GetMeetings,
    private val getLastMessageUseCase: GetLastMessageUseCase,
    private val meetingLastTimestampMapper: MeetingLastTimestampMapper,
    private val scheduledMeetingTimestampMapper: ScheduledMeetingTimestampMapper,
    private val deviceGateway: DeviceGateway,
    @DefaultDispatcher private val dispatcher: CoroutineDispatcher,
) : BaseRxViewModel() {

    private var queryString: String? = null
    private val meetings: MutableLiveData<List<MeetingRoomItem>> = MutableLiveData(emptyList())
    private val is24HourFormat by lazy { deviceGateway.is24HourFormat() }

    private val mutex = Mutex()

    init {
        retrieveMeetings()
        signalChatPresence()
    }

    private fun retrieveMeetings() {
        viewModelScope.launch(dispatcher) {
            getMeetingsUseCase(mutex)
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
                .flowOn(dispatcher)
                .catch { Timber.e(it) }
                .collectLatest(meetings::postValue)
        }
    }

    /**
     * Get meetings
     *
     * @return  LiveData with a list of filtered MeetingRoomItem
     */
    fun getMeetings(): LiveData<List<MeetingRoomItem>> =
        meetings.map { items ->
            val searchQuery = queryString
            if (!searchQuery.isNullOrBlank() && !items.isNullOrEmpty()) {
                items.filter { (_, title, lastMessage, _, _, _, _, _, _, _, _, _, _) ->
                    title.contains(searchQuery, true)
                            || lastMessage?.contains(searchQuery, true) == true
                }
            } else {
                items
            }
        }

    /**
     * Check if search query is empty
     *
     * @return  true if search query is empty, false otherwise
     */
    fun isSearchQueryEmpty(): Boolean =
        queryString.isNullOrBlank()

    /**
     * Get specific meeting given its chat id
     *
     * @param chatId    Chat id to identify chat
     * @return          LiveData with MeetingRoomItem
     */
    fun getMeeting(chatId: Long): LiveData<MeetingRoomItem?> =
        meetings.map { meeting -> meeting.find { it.chatId == chatId } }

    /**
     * Set search query string
     *
     * @param query Search query
     */
    fun setSearchQuery(query: String?) {
        queryString = query
        meetings.notifyObserver()
    }

    /**
     * Archive chat
     *
     * @param chatId    Chat id to be archived
     */
    fun archiveChat(chatId: Long) {
        archiveChatUseCase.archive(chatId, true)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = Timber::e)
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
        leaveChatUseCase.leave(chatId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = Timber::e)
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
        signalChatPresenceUseCase.signal()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = Timber::e)
            .addTo(composite)
    }
}
