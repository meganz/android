package mega.privacy.android.app.meeting.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.usecase.chat.ArchiveChatUseCase
import mega.privacy.android.app.usecase.chat.ClearChatHistoryUseCase
import mega.privacy.android.app.usecase.chat.LeaveChatUseCase
import mega.privacy.android.app.usecase.chat.SignalChatPresenceUseCase
import mega.privacy.android.app.usecase.meeting.GetMeetingListUseCase
import mega.privacy.android.app.utils.RxUtil.debounceImmediate
import mega.privacy.android.app.utils.notifyObserver
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Meeting list view model
 *
 * @property getMeetingListUseCase      Use case to retrieve meeting list
 * @property archiveChatUseCase         Use case to archive chats
 * @property leaveChatUseCase           Use case to leave chats
 * @property signalChatPresenceUseCase  Use case to signal chat presence
 * @property clearChatHistoryUseCase    Use case to clear chat history
 */
@HiltViewModel
class MeetingListViewModel @Inject constructor(
    private val getMeetingListUseCase: GetMeetingListUseCase,
    private val archiveChatUseCase: ArchiveChatUseCase,
    private val leaveChatUseCase: LeaveChatUseCase,
    private val signalChatPresenceUseCase: SignalChatPresenceUseCase,
    private val clearChatHistoryUseCase: ClearChatHistoryUseCase,
) : BaseRxViewModel() {

    companion object {
        private const val REQUEST_TIMEOUT_IN_MS = 100L
    }

    private var queryString: String? = null
    private val meetings: MutableLiveData<List<MeetingItem.Data>> = MutableLiveData(emptyList())

    init {
        retrieveMeetings()
        signalChatPresence()
    }

    private fun retrieveMeetings() {
        getMeetingListUseCase.get()
            .debounceImmediate(REQUEST_TIMEOUT_IN_MS, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { items ->
                    meetings.value = items.toList()
                },
                onError = Timber::e
            )
            .addTo(composite)
    }

    /**
     * Get meetings
     *
     * @return  LiveData with a list of MeetingItem.Data
     */
    fun getMeetings(): LiveData<List<MeetingItem.Data>> =
        meetings.map { items ->
            val searchQuery = queryString
            if (!searchQuery.isNullOrBlank() && !items.isNullOrEmpty()) {
                items.filter { (_, title, lastMessage, _, _, _, _, _, firstUser, lastUser, _, _, _) ->
                    title.contains(searchQuery, true)
                            || lastMessage?.contains(searchQuery, true) == true
                            || firstUser.firstName?.contains(searchQuery, true) == true
                            || lastUser?.firstName?.contains(searchQuery, true) == true
                            || firstUser.email?.contains(searchQuery, true) == true
                            || lastUser?.email?.contains(searchQuery, true) == true
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
     * @return          LiveData with MeetingItem.Data
     */
    fun getMeeting(chatId: Long): LiveData<MeetingItem.Data?> =
        meetings.map { meeting -> meeting.find { it.id == chatId } }

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
     * Clear chat history
     *
     * @param chatId    Chat id to leave
     */
    fun clearChatHistory(chatId: Long) {
        clearChatHistoryUseCase.clear(chatId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(onError = Timber::e)
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
