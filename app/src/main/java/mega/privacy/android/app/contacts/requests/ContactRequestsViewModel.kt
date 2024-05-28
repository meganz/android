package mega.privacy.android.app.contacts.requests

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestPageAdapter.Tabs.INCOMING
import mega.privacy.android.app.contacts.requests.adapter.ContactRequestPageAdapter.Tabs.OUTGOING
import mega.privacy.android.app.contacts.requests.data.ContactRequestItem
import mega.privacy.android.app.contacts.requests.data.ContactRequestsState
import mega.privacy.android.app.contacts.requests.mapper.ContactRequestItemMapper
import mega.privacy.android.app.contacts.usecase.ManageContactRequestUseCase
import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import mega.privacy.android.domain.entity.contacts.ContactRequestLists
import mega.privacy.android.domain.usecase.account.contactrequest.MonitorContactRequestsUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel that handles all related logic to Contact Groups for the current user.
 *
 * @param manageContactRequestUseCase    Use case to reply to existing contact requests
 * @param monitorContactRequestsUseCase  Use case to monitor contact requests
 * @param contactRequestItemMapper       Mapper to map ContactRequest to ContactRequestItem
 */
@HiltViewModel
internal class ContactRequestsViewModel @Inject constructor(
    private val manageContactRequestUseCase: ManageContactRequestUseCase,
    private val monitorContactRequestsUseCase: MonitorContactRequestsUseCase,
    private val contactRequestItemMapper: ContactRequestItemMapper,
) : ViewModel() {

    private val composite = CompositeDisposable()

    private val _state = MutableStateFlow<ContactRequestsState>(
        ContactRequestsState.Empty
    )
    private val queryFlow: MutableStateFlow<String?> = MutableStateFlow(null)

    init {
        viewModelScope.launch {
            runCatching {
                monitorContactRequestsUseCase()
                    .mapToUIModels()
                    .combine(queryFlow) { requests, query ->
                        val incoming = requests.first.filterByQuery(query)
                        val outgoing = requests.second.filterByQuery(query)
                        ContactRequestsState.Data(
                            items = incoming + outgoing,
                            incoming = incoming,
                            outGoing = outgoing,
                        )
                    }
                    .catch { Timber.e(it) }
                    .collect {
                        _state.emit(it)
                    }
            }.onFailure {
                Timber.e(it)
            }
        }
    }

    private fun Flow<ContactRequestLists>.mapToUIModels() =
        map { (incoming, outgoing) ->
            Pair(
                incoming.mapNotNull { contactRequestItemMapper(it) },
                outgoing.mapNotNull { contactRequestItemMapper(it) })
        }

    private fun List<ContactRequestItem>.filterByQuery(query: String?) =
        query?.let {
            filter { item ->
                item.email.contains(it, true)
            }
        } ?: this

    fun getIncomingRequest(): LiveData<List<ContactRequestItem>> =
        _state.mapNotNull { (it as? ContactRequestsState.Data)?.incoming }.asLiveData()

    fun getOutgoingRequest(): LiveData<List<ContactRequestItem>> =
        _state.mapNotNull { (it as? ContactRequestsState.Data)?.outGoing }.asLiveData()

    fun getContactRequest(requestHandle: Long): LiveData<ContactRequestItem?> =
        _state.mapNotNull { (it as? ContactRequestsState.Data)?.items?.find { item -> item.handle == requestHandle } }
            .asLiveData()

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
        viewModelScope.launch {
            queryFlow.emit(query)
        }
    }

    /**
     * Retrieve ViewPager's default position based on contact requests size and current outgoing value.
     *
     * @param isOutgoing    Whether the current view is for outgoing requests or not.
     * @return              LiveData with ViewPager's desired position.
     */
    fun getDefaultPagerPosition(isOutgoing: Boolean): LiveData<Int> =
        _state.map {
            when {
                isOutgoing && it.hasOutgoing -> OUTGOING.ordinal
                !isOutgoing && it.hasIncoming -> INCOMING.ordinal
                isOutgoing && it.hasIncoming -> INCOMING.ordinal
                !isOutgoing && it.hasOutgoing -> OUTGOING.ordinal
                isOutgoing -> OUTGOING.ordinal
                else -> INCOMING.ordinal
            }
        }.asLiveData(context = viewModelScope.coroutineContext)

    override fun onCleared() {
        super.onCleared()
        composite.clear()
    }
}
