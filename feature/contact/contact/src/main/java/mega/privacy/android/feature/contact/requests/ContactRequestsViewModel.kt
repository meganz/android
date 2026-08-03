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
 * Monitors the incoming and outgoing contact requests, maps them to UI rows and combines them with
 * the currently selected tab (seeded from the tab the screen was opened on). Handling an action
 * replies to the request; the monitor flow re-emits after the SDK applies the change, so the list
 * refreshes on its own.
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

    /**
     * UI state for the contact requests screen.
     */
    val uiState: StateFlow<ContactRequestsUiState> by lazy {
        combine(
            monitorContactRequestsUseCase().map { lists ->
                lists.incomingContactRequests.map(contactRequestUiItemMapper::invoke) to
                        lists.outgoingContactRequests.map(contactRequestUiItemMapper::invoke)
            },
            selectedTab.asStateFlow(),
        ) { (received, sent), tab ->
            ContactRequestsUiState.Data(
                received = received.toImmutableList(),
                sent = sent.toImmutableList(),
                selectedTab = tab,
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
