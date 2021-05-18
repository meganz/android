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
import mega.privacy.android.app.contacts.usecase.GetContactsUseCase
import mega.privacy.android.app.utils.notifyObserver

class ContactListViewModel @ViewModelInject constructor(
    getContactsUseCase: GetContactsUseCase
) : BaseRxViewModel() {

    companion object {
        private const val TAG = "ContactListViewModel"
    }

    private val contacts: MutableLiveData<List<ContactItem>> = MutableLiveData()
    private var queryString: String? = null

    init {
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

    fun getContacts(): LiveData<List<ContactItem>> =
        contacts.map { items ->
            if (queryString.isNullOrBlank()) {
                items
            } else {
                items.filter { it.matches(queryString!!) }
            }
        }

    fun getRecentlyAddedContacts(): LiveData<List<ContactItem>> =
        contacts.map { items ->
            if (queryString.isNullOrBlank()) {
                items.filter { it.isNew }
            } else {
                emptyList()
            }
        }

    fun setQuery(query: String?) {
        queryString = query
        contacts.notifyObserver()
    }
}
