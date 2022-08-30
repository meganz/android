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
    private val meetings: MutableLiveData<List<MeetingItem>> = MutableLiveData()

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
     * @return  LiveData with a list of MeetingItem
     */
    fun getMeetings(): LiveData<List<MeetingItem>> =
        meetings.map { items ->
            if (!queryString.isNullOrBlank()) {
                items.filter { (_, title, lastMessage, _, _, _, firstUser, lastUser, _, _, _) ->
                    title.contains(queryString!!, true)
                            || lastMessage?.contains(queryString!!, true) == true
                            || firstUser.firstName?.contains(queryString!!, true) == true
                            || lastUser?.firstName?.contains(queryString!!, true) == true
                            || firstUser.email?.contains(queryString!!, true) == true
                            || lastUser?.email?.contains(queryString!!, true) == true
                }
            } else {
                items
            }
        }

    /**
     * Get specific meeting given its chat id
     *
     * @param chatId    Chat id to identify chat
     * @return          LiveData with MeetingItem
     */
    fun getMeeting(chatId: Long): LiveData<MeetingItem?> =
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
