package mega.privacy.android.app.contacts.list

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.contacts.list.data.ContactItem
import mega.privacy.android.app.contacts.usecase.GetContactRequestsUseCase
import mega.privacy.android.app.contacts.usecase.GetContactsUseCase
import mega.privacy.android.app.utils.notifyObserver

class ContactListViewModel @ViewModelInject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val getContactRequestsUseCase: GetContactRequestsUseCase
) : BaseRxViewModel() {

    companion object {
        private const val TAG = "ContactListViewModel"
    }

    private val contacts: MutableLiveData<List<ContactItem.Data>> = MutableLiveData()
    private val incomingRequestsSize: MutableLiveData<Int> = MutableLiveData(0)
    private var queryString: String? = null

    init {
        retrieveContacts()
        retrieveIncomingContactRequestsSize()
    }

    fun retrieveContacts() {
        composite.clear()
        getContactsUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { items ->
                    contacts.value = items.toList()
                },
                onError = { error ->
                    Log.e(TAG, error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    private fun retrieveIncomingContactRequestsSize() {
        getContactRequestsUseCase.getIncomingRequestsSize()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { size ->
                    incomingRequestsSize.value = size
                },
                onError = { error ->
                    Log.e(TAG, error.stackTraceToString())
                }
            )
            .addTo(composite)
    }

    fun getIncomingContactRequestsSize(): LiveData<Int> = incomingRequestsSize

    fun getContactsWithHeaders(headerTitle: String): LiveData<List<ContactItem>> =
        contacts.map { items ->
            val itemsWithHeaders = mutableListOf<ContactItem>()
            items?.forEachIndexed { index, item ->
                if (queryString.isNullOrBlank()) {
                    when {
                        index == 0 -> {
                            if (items.firstOrNull { it.isNew } != null) {
                                itemsWithHeaders.add(ContactItem.Header(headerTitle))
                            }
                            itemsWithHeaders.add(ContactItem.Header(item.getFirstCharacter()))
                        }
                        items[index - 1].getFirstCharacter() != items[index].getFirstCharacter() -> {
                            itemsWithHeaders.add(ContactItem.Header(item.getFirstCharacter()))
                        }
                    }
                    itemsWithHeaders.add(item)
                } else if (item.matches(queryString!!)) {
                    itemsWithHeaders.add(item)
                }
            }
            itemsWithHeaders
        }

    fun getRecentlyAddedContacts(headerTitle: String): LiveData<List<ContactItem>> =
        contacts.map { items ->
            if (queryString.isNullOrBlank() && items.firstOrNull { it.isNew } != null) {
                val itemsWithHeaders = mutableListOf<ContactItem>()
                itemsWithHeaders.add(ContactItem.Header(headerTitle))
                itemsWithHeaders.addAll(items.filter { it.isNew })
                itemsWithHeaders
            } else {
                emptyList()
            }
        }

    fun setQuery(query: String?) {
        queryString = query
        contacts.notifyObserver()
    }
}
