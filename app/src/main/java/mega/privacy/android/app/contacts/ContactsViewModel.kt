package mega.privacy.android.app.contacts

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
import mega.privacy.android.app.contacts.data.ContactItem
import mega.privacy.android.app.contacts.usecase.GetContactsUseCase

class ContactsViewModel @ViewModelInject constructor(
    private val getContactsUseCase: GetContactsUseCase
) : BaseRxViewModel() {

    companion object {
        private const val TAG = "ContactsViewModel"
    }

    private val contacts: MutableLiveData<List<ContactItem>> = MutableLiveData()

    init {
        updateContacts()
    }

    fun getContacts(): LiveData<List<ContactItem>> = contacts

    fun getRecentlyAddedContacts(): LiveData<List<ContactItem>> = contacts
        .map { contact -> contact.filter { it.isNew } }

    private fun updateContacts() {
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
}
