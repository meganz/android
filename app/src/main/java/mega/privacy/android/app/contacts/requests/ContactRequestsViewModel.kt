package mega.privacy.android.app.contacts.requests

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestPageAdapter.Tabs.INCOMING
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestPageAdapter.Tabs.OUTGOING
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.contacts.usecase.GetContactRequestsUseCase
import mega.privacy.android.app.contacts.usecase.ManageContactRequestUseCase
import mega.privacy.android.app.utils.notifyObserver
import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import mega.privacy.android.domain.usecase.account.contactrequest.MonitorContactRequestsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel that handles all related logic to Contact Groups for the current user.
 *
 * @param getContactRequestsUseCase     Use case to retrieve contact requests for current user
 * @param manageContactRequestUseCase    Use case to reply to existing contact requests
 */
@HiltViewModel
class ContactRequestsViewModel @Inject constructor(
    private val getContactRequestsUseCase: GetContactRequestsUseCase,
    private val manageContactRequestUseCase: ManageContactRequestUseCase,
    private val monitorContactRequestsUseCase: MonitorContactRequestsUseCase,
) : ViewModel() {

    private val composite = CompositeDisposable()

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
                onError = {
                    Timber.e(it)
                }
            )
            .addTo(composite)
    }

    fun getFilteredContactRequests(): LiveData<List<ContactRequestItem>> =
        contactRequests.map { items ->
            val query = queryString
            if (!query.isNullOrBlank()) {
                items.filter { item -> item.email.contains(query, true) }
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

    /**
     * Handle contact request with a specific action
     *
     * @param requestHandle         contact request identifier
     * @param contactRequestAction  contact request action
     */
    fun handleContactRequest(requestHandle: Long, contactRequestAction: ContactRequestAction) {
        viewModelScope.launch {
            runCatching {
                manageContactRequestUseCase(requestHandle, contactRequestAction)
            }.onFailure {
                Timber.e(it)
            }
        }
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
    fun getDefaultPagerPosition(isOutgoing: Boolean): LiveData<Int> =
        monitorContactRequestsUseCase()
            .map {
                val hasIncoming = it.incomingContactRequests.isNotEmpty()
                val hasOutGoing = it.outgoingContactRequests.isNotEmpty()
                when {
                    isOutgoing && hasOutGoing -> OUTGOING.ordinal
                    !isOutgoing && hasIncoming -> INCOMING.ordinal
                    isOutgoing && hasIncoming -> INCOMING.ordinal
                    !isOutgoing && hasOutGoing -> OUTGOING.ordinal
                    isOutgoing -> OUTGOING.ordinal
                    else -> INCOMING.ordinal
                }
            }.asLiveData(context = viewModelScope.coroutineContext)

    override fun onCleared() {
        super.onCleared()
        composite.clear()
    }
}
