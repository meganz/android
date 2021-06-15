package mega.privacy.android.app.contacts.requests

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.contacts.usecase.GetContactRequestsUseCase
import mega.privacy.android.app.contacts.usecase.ReplyContactRequestUseCase
import mega.privacy.android.app.utils.notifyObserver

class ContactRequestsViewModel @ViewModelInject constructor(
    private val getContactRequestsUseCase: GetContactRequestsUseCase,
    private val replyContactRequestUseCase: ReplyContactRequestUseCase
) : BaseRxViewModel() {

    companion object {
        private const val TAG = "ContactRequestViewModel"
    }

    private val contactRequests: MutableLiveData<List<ContactRequestItem>> = MutableLiveData()
    private var queryString: String? = null

    init {
        updateRequests()
    }

    fun updateRequests() {
        composite.clear()
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

    fun getFilteredContactRequests(): LiveData<List<ContactRequestItem>> =
        contactRequests.map { items ->
            if (!queryString.isNullOrBlank()) {
                items.filter { item ->
                    item.name?.contains(queryString!!, true) == true
                            || item.email?.contains(queryString!!, true) == true
                }
            } else {
                items
            }
        }

    fun getIncomingRequest(): LiveData<List<ContactRequestItem>> =
        getFilteredContactRequests().map { it.filter { item -> !item.isOutgoing } }

    fun getOutgoingRequest(): LiveData<List<ContactRequestItem>> =
        getFilteredContactRequests().map { it.filter { item -> item.isOutgoing } }

    fun getContactRequest(requestHandle: Long): LiveData<ContactRequestItem?> =
        getFilteredContactRequests().map { it.find { item -> item.handle == requestHandle } }

    fun acceptRequest(requestHandle: Long) {
        replyContactRequestUseCase.acceptReceivedRequest(requestHandle).subscribeAndUpdate()
    }

    fun ignoreRequest(requestHandle: Long) {
        replyContactRequestUseCase.ignoreReceivedRequest(requestHandle).subscribeAndUpdate()
    }

    fun declineRequest(requestHandle: Long) {
        replyContactRequestUseCase.denyReceivedRequest(requestHandle).subscribeAndUpdate()
    }

    fun reinviteRequest(requestHandle: Long) {
        replyContactRequestUseCase.remindSentRequest(requestHandle).subscribeAndUpdate()
    }

    fun removeRequest(requestHandle: Long) {
        replyContactRequestUseCase.deleteSentRequest(requestHandle).subscribeAndUpdate()
    }

    fun setQuery(query: String?) {
        queryString = query
        contactRequests.notifyObserver()
    }

    private fun Completable.subscribeAndUpdate() {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { updateRequests() },
                onError = { Log.e(TAG, it.stackTraceToString()) }
            )
            .addTo(composite)
    }
}
