package mega.privacy.android.app.contacts.group

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.contacts.group.data.ContactGroupItem
import mega.privacy.android.app.contacts.usecase.CreateGroupChatUseCase
import mega.privacy.android.app.contacts.usecase.GetContactGroupsUseCase
import mega.privacy.android.app.utils.LogUtil.logError
import mega.privacy.android.app.utils.notifyObserver
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import java.util.ArrayList

/**
 * ViewModel that handles all related logic to Contact Groups for the current user.
 *
 * @param getContactGroupsUseCase   UseCase to retrieve all contact groups.
 */
class ContactGroupsViewModel @ViewModelInject constructor(
    getContactGroupsUseCase: GetContactGroupsUseCase,
    private val getGroupChatRoomUseCase: CreateGroupChatUseCase
) : BaseRxViewModel() {

    private val groups: MutableLiveData<List<ContactGroupItem>> = MutableLiveData()
    private var queryString: String? = null

    init {
        getContactGroupsUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { items ->
                    groups.value = items.toList()
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun getGroups(): LiveData<List<ContactGroupItem>> =
        groups.map { items ->
            if (!queryString.isNullOrBlank()) {
                items.filter { it.title.contains(queryString!!, true) }
            } else {
                items
            }
        }

    fun setQuery(query: String?) {
        queryString = query
        groups.notifyObserver()
    }

    /**
     * Method for creating a group chat room when multiple contacts have been selected
     *
     * @param contactsData List of contacts who will participate in the chat
     * @param chatTitle Title of the group chat room
     * @return chat ID of the new group chat room
     */
    fun getGroupChatRoom(
        contactsData: ArrayList<String>,
        chatTitle: String?
    ): LiveData<Long> {
        val result = MutableLiveData<Long>()
        getGroupChatRoomUseCase.getGroupChatRoomCreated(contactsData, chatTitle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { chatId ->
                    result.value = chatId
                },
                onError = { error ->
                    logError(error.stackTraceToString())
                    result.value = MEGACHAT_INVALID_HANDLE
                }
            )
            .addTo(composite)
        return result
    }
}
