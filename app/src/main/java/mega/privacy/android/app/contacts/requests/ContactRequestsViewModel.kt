package mega.privacy.android.app.contacts.requests

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
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.contacts.usecase.GetContactRequestsUseCase

class ContactRequestsViewModel @ViewModelInject constructor(
    getContactRequestsUseCase: GetContactRequestsUseCase
) : BaseRxViewModel() {

    companion object {
        private const val TAG = "ContactRequestViewModel"
    }

    private val contactRequests: MutableLiveData<List<ContactRequestItem>> = MutableLiveData()

    fun getIncomingRequest(): LiveData<List<ContactRequestItem>> = contactRequests.map { it.filter { item -> !item.isOutgoing } }
    fun getOutgoingRequest(): LiveData<List<ContactRequestItem>> = contactRequests.map { it.filter { item -> item.isOutgoing } }

    init {
        getContactRequestsUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { items ->
                    contactRequests.value = items
                },
                onError = { error ->
                    Log.e(TAG, error.stackTraceToString())
                }
            )
            .addTo(composite)
    }
}
