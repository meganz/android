package mega.privacy.android.app.contacts.group

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import mega.privacy.android.app.contacts.group.data.ContactGroupItem
import mega.privacy.android.app.contacts.usecase.GetContactGroupsUseCase
import mega.privacy.android.app.utils.notifyObserver
import mega.privacy.android.domain.usecase.chat.CreateGroupChatRoomUseCase
import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel that handles all related logic to Contact Groups for the current user.
 *
 * @param getContactGroupsUseCase   UseCase to retrieve all contact groups.
 */
@HiltViewModel
class ContactGroupsViewModel @Inject constructor(
    getContactGroupsUseCase: GetContactGroupsUseCase,
    private val createGroupChatRoomUseCase: CreateGroupChatRoomUseCase,
) : ViewModel() {

    private val groups: MutableLiveData<List<ContactGroupItem>> = MutableLiveData()
    private var queryString: String? = null

    init {
        viewModelScope.launch {
            runCatching {
                getContactGroupsUseCase()
            }.onSuccess {
                groups.value = it
            }.onFailure {
                Timber.e(it)
            }
        }
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
        groups.takeIf { it.value != null }?.notifyObserver()
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
        chatTitle: String?,
        allowAddParticipants: Boolean,
    ): LiveData<Long> {
        val result = MutableLiveData<Long>()
        viewModelScope.launch {
            runCatching {
                createGroupChatRoomUseCase(
                    contactsData,
                    chatTitle,
                    false,
                    allowAddParticipants,
                    false
                )
            }.onSuccess { chatId ->
                result.value = chatId
            }.onFailure {
                Timber.e(it)
                result.value = MEGACHAT_INVALID_HANDLE
            }
        }

        return result
    }
}
