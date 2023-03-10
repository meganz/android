package mega.privacy.android.app.contacts.requests

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestPageAdapter.Tabs.INCOMING
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestPageAdapter.Tabs.OUTGOING
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.contacts.usecase.GetContactRequestsUseCase
import mega.privacy.android.app.contacts.usecase.ReplyContactRequestUseCase
import mega.privacy.android.app.utils.notifyObserver
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel that handles all related logic to Contact Groups for the current user.
 *
 * @param getContactRequestsUseCase     Use case to retrieve contact requests for current user
 * @param replyContactRequestUseCase    Use case to reply to existing contact requests
 */
@HiltViewModel
class ContactRequestsViewModel @Inject constructor(
    private val getContactRequestsUseCase: GetContactRequestsUseCase,
    private val replyContactRequestUseCase: ReplyContactRequestUseCase
) : BaseRxViewModel() {

    private val contactRequests: MutableLiveData<List<ContactRequestItem>> = MutableLiveData()
    private var queryString: String? = null

    init {
        getContactRequestsUseCase.get()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onNext = { items ->
                    contactRequests.value = items
                },
                onError = Timber::e
            )
            .addTo(composite)
    }

    fun getFilteredContactRequests(): LiveData<List<ContactRequestItem>> =
        contactRequests.map { items ->
            val query = queryString
            if (!query.isNullOrBlank()) {
                items.filter { item ->
                    item.name?.contains(query, true) == true || item.email.contains(query, true)
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

    /**
     * Retrieve ViewPager's default position based on contact requests size and current outgoing value.
     *
     * @param isOutgoing    Whether the current view is for outgoing requests or not.
     * @return              LiveData with ViewPager's desired position.
     */
    fun getDefaultPagerPosition(isOutgoing: Boolean): LiveData<Int> {
        val result = MutableLiveData<Int>()
        getContactRequestsUseCase.getRequestsSize()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onSuccess = { requestsSize ->
                    result.value = if (isOutgoing) {
                        when {
                            requestsSize.second > 0 -> OUTGOING.ordinal
                            requestsSize.first > 0 -> INCOMING.ordinal
                            else -> OUTGOING.ordinal
                        }
                    } else {
                        when {
                            requestsSize.first > 0 -> INCOMING.ordinal
                            requestsSize.second > 0 -> OUTGOING.ordinal
                            else -> INCOMING.ordinal
                        }
                    }
                },
                onError = Timber::e)
            .addTo(composite)
        return result
    }

    private fun Completable.subscribeAndUpdate() {
        subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = Timber::e
            )
            .addTo(composite)
    }
}
