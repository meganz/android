package mega.privacy.android.feature.contact.requests

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import mega.privacy.android.domain.usecase.account.contactrequest.MonitorContactRequestsUseCase
import mega.privacy.android.domain.usecase.account.contactrequest.ReplyContactRequestUseCase
import mega.privacy.android.feature.contact.requests.mapper.ContactRequestUiItemMapper
import mega.privacy.android.feature.contact.requests.model.ContactRequestTab
import mega.privacy.android.feature.contact.requests.model.ContactRequestUiItem
import mega.privacy.android.feature.contact.requests.model.ContactRequestsUiState
import timber.log.Timber

/**
 * ViewModel for the contact requests screen.
 *
 * Monitors the incoming and outgoing contact requests, maps them to UI rows, filters them by the
 * current search query and combines them with the currently selected tab. The tab the screen was
 * opened on is only a seed: on the first data emission it yields to the other tab when it is empty
 * and the other tab is not, after which only the user changes it. Handling an action replies to
 * the request; the monitor flow re-emits after the SDK applies the change, so the list refreshes
 * on its own.
 *
 * @property monitorContactRequestsUseCase
 * @property replyContactRequestUseCase
 * @property contactRequestUiItemMapper
 */
@HiltViewModel(assistedFactory = ContactRequestsViewModel.Factory::class)
class ContactRequestsViewModel @AssistedInject constructor(
    @Assisted initialTab: ContactRequestTab,
    private val monitorContactRequestsUseCase: MonitorContactRequestsUseCase,
    private val replyContactRequestUseCase: ReplyContactRequestUseCase,
    private val contactRequestUiItemMapper: ContactRequestUiItemMapper,
) : ViewModel() {

    private val selectedTab = MutableStateFlow(initialTab)
    private val searchQuery = MutableStateFlow<String?>(null)
    private var initialTabResolved = false

    /**
     * UI state for the contact requests screen.
     */
    val uiState: StateFlow<ContactRequestsUiState> by lazy {
        combine(
            monitorContactRequestsUseCase().map { lists ->
                lists.incomingContactRequests.map(contactRequestUiItemMapper::invoke) to
                        lists.outgoingContactRequests.map(contactRequestUiItemMapper::invoke)
            },
            searchQuery.asStateFlow(),
            selectedTab.asStateFlow(),
        ) { (received, sent), query, tab ->
            ContactRequestsUiState.Data(
                received = received.filter { it.matches(query) }.toImmutableList(),
                sent = sent.filter { it.matches(query) }.toImmutableList(),
                selectedTab = resolveInitialTab(tab, received, sent),
                query = query,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, ContactRequestsUiState.Loading)
    }

    /**
     * Select the given tab.
     *
     * @param tab tab to show.
     */
    fun selectTab(tab: ContactRequestTab) {
        selectedTab.value = tab
    }

    /**
     * Set the search query.
     *
     * @param query text to filter the requests by, or null to show them all.
     */
    fun setQuery(query: String?) {
        searchQuery.value = query
    }

    /**
     * Apply an action to a contact request.
     *
     * @param item the request row the action was triggered on.
     * @param action the action to apply.
     */
    fun handleAction(item: ContactRequestUiItem, action: ContactRequestAction) {
        viewModelScope.launch {
            runCatching {
                replyContactRequestUseCase(item.handle, action, item.isOutgoing)
            }.onFailure { Timber.e(it) }
        }
    }

    /**
     * Applied to the unfiltered lists exactly once so that a later data change or a user tab
     * selection is never overridden. Precedence: the seeded tab if it has requests, else the other
     * tab if it has requests, else the seeded tab.
     */
    private fun resolveInitialTab(
        tab: ContactRequestTab,
        received: List<ContactRequestUiItem>,
        sent: List<ContactRequestUiItem>,
    ): ContactRequestTab {
        if (initialTabResolved) return tab
        initialTabResolved = true
        val other = when (tab) {
            ContactRequestTab.Received -> ContactRequestTab.Sent
            ContactRequestTab.Sent -> ContactRequestTab.Received
        }
        val resolved = when {
            tab.requestsIn(received, sent).isNotEmpty() -> tab
            other.requestsIn(received, sent).isNotEmpty() -> other
            else -> tab
        }
        selectedTab.value = resolved
        return resolved
    }

    private fun ContactRequestTab.requestsIn(
        received: List<ContactRequestUiItem>,
        sent: List<ContactRequestUiItem>,
    ): List<ContactRequestUiItem> = when (this) {
        ContactRequestTab.Received -> received
        ContactRequestTab.Sent -> sent
    }

    private fun ContactRequestUiItem.matches(query: String?): Boolean {
        if (query.isNullOrBlank()) return true
        return contact.displayName.contains(query, ignoreCase = true)
                || contact.email.contains(query, ignoreCase = true)
    }

    /**
     * Assisted factory for [ContactRequestsViewModel].
     */
    @AssistedFactory
    interface Factory {
        /**
         * Create a [ContactRequestsViewModel].
         *
         * @param initialTab the tab the screen opens on.
         */
        fun create(initialTab: ContactRequestTab): ContactRequestsViewModel
    }
}
