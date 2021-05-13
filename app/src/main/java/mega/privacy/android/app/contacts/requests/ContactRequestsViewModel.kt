package mega.privacy.android.app.contacts.requests

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.contacts.data.ContactItem
import mega.privacy.android.app.contacts.usecase.GetContactRequestsUseCase

class ContactRequestsViewModel @ViewModelInject constructor(
    getContactRequestsUseCase: GetContactRequestsUseCase
) : BaseRxViewModel() {

    companion object {
        private const val TAG = "ContactRequestViewModel"
    }

    private val incomingRequestContacts: MutableLiveData<List<ContactItem>> = MutableLiveData()
    private val outgoingRequestContacts: MutableLiveData<List<ContactItem>> = MutableLiveData()

    fun getIncomingRequestContacts(): LiveData<List<ContactItem>> = incomingRequestContacts
    fun getOutgoingRequestContacts(): LiveData<List<ContactItem>> = outgoingRequestContacts

    init {
        getContactRequestsUseCase.getIncomingRequests()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { items ->
                    incomingRequestContacts.value = items
                },
                onError = { error ->
                    Log.e(TAG, error.stackTraceToString())
                }
            )
            .addTo(composite)

        getContactRequestsUseCase.getOutgoingRequests()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { items ->
                    outgoingRequestContacts.value = items
                },
                onError = { error ->
                    Log.e(TAG, error.stackTraceToString())
                }
            )
            .addTo(composite)
    }
}
