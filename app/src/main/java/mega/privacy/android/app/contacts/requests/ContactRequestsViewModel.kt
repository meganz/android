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
import mega.privacy.android.app.contacts.usecase.ReplyContactRequestUseCase

class ContactRequestsViewModel @ViewModelInject constructor(
    private val getContactRequestsUseCase: GetContactRequestsUseCase,
    private val replyContactRequestUseCase: ReplyContactRequestUseCase
) : BaseRxViewModel() {

    companion object {
        private const val TAG = "ContactRequestViewModel"
    }

    private val contactRequests: MutableLiveData<List<ContactRequestItem>> = MutableLiveData()

    init {
        getContactRequests()
    }

    private fun getContactRequests() {
        getContactRequestsUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { items -> contactRequests.value = items },
                onError = { error -> Log.e(TAG, error.stackTraceToString()) }
            )
            .addTo(composite)
    }

    fun getIncomingRequest(): LiveData<List<ContactRequestItem>> =
        contactRequests.map { it.filter { item -> !item.isOutgoing } }

    fun getOutgoingRequest(): LiveData<List<ContactRequestItem>> =
        contactRequests.map { it.filter { item -> item.isOutgoing } }

    fun getContactRequest(requestHandle: Long) =
        contactRequests.map { it.find { item -> item.handle == requestHandle } }

    fun acceptRequest(requestHandle: Long) {
        replyContactRequestUseCase.acceptReceivedRequest(requestHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { getContactRequests() },
                onError = { Log.e(TAG, it.stackTraceToString()) }
            )
            .addTo(composite)
    }

    fun ignoreRequest(requestHandle: Long) {
        replyContactRequestUseCase.ignoreReceivedRequest(requestHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { getContactRequests() },
                onError = { Log.e(TAG, it.stackTraceToString()) }
            )
            .addTo(composite)
    }

    fun declineRequest(requestHandle: Long) {
        replyContactRequestUseCase.denyReceivedRequest(requestHandle)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onComplete = { getContactRequests() },
                onError = { Log.e(TAG, it.stackTraceToString()) }
            )
            .addTo(composite)
    }
}
