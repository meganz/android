package mega.privacy.android.feature.contact.add

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.usecase.contact.GetContactsToAddToChatUseCase
import mega.privacy.android.domain.usecase.contact.GetContactsUseCase
import mega.privacy.android.feature.contact.add.model.AddContactUiState
import mega.privacy.android.shared.contact.mapper.ContactItemUiStateMapper
import mega.privacy.android.shared.contact.model.ContactItemUiState
import timber.log.Timber

/**
 * Add contact view model. Backs the MEGA-contacts multi-select picker: exposes the
 * searchable contact list and resolves the selected handles to emails on confirm.
 * Selection itself is owned by the Compose layer, not this ViewModel.
 *
 * When [chatId] is provided the contacts already participating in that chat are excluded,
 * which backs the "add chat participants" flow; otherwise the full visible contact list is shown.
 *
 * @property chatId optional chat whose existing participants should be excluded.
 * @property getContactsUseCase
 * @property getContactsToAddToChatUseCase
 * @property contactItemUiStateMapper
 */
@HiltViewModel(assistedFactory = AddContactViewModel.Factory::class)
class AddContactViewModel @AssistedInject constructor(
    @Assisted private val chatId: Long?,
    private val getContactsUseCase: GetContactsUseCase,
    private val getContactsToAddToChatUseCase: GetContactsToAddToChatUseCase,
    private val contactItemUiStateMapper: ContactItemUiStateMapper,
) : ViewModel() {

    /**
     * Factory for assisted creation, supplying the optional [chatId] from the navigation key.
     */
    @AssistedFactory
    interface Factory {
        /**
         * @param chatId the chat whose existing participants to exclude, or null for the full list.
         */
        fun create(chatId: Long?): AddContactViewModel
    }

    private val queryChannel = Channel<String?>(Channel.CONFLATED)

    private fun contactsSource(): Flow<List<ContactItem>> =
        chatId?.let { getContactsToAddToChatUseCase(it) } ?: getContactsUseCase()

    /**
     * Resolves a selected contact handle back to its email. Retained from the full
     * (unfiltered) contact list so a selected contact still resolves even when it has
     * been filtered out of the visible list by the current search query.
     */
    private var handleToEmail: Map<Long, String> = emptyMap()

    /**
     * Ui state
     */
    val uiState: StateFlow<AddContactUiState> by lazy {
        combine(
            queryChannel.receiveAsFlow().onStart { emit(null) },
            contactsSource().map { domainList ->
                domainList
                    .map { item ->
                        IndexedContact(
                            data = item,
                            ui = contactItemUiStateMapper(item)
                        )
                    }
                    .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.ui.displayName })
                    .also { indexed -> handleToEmail = indexed.associate { it.ui.handle to it.data.email } }
            },
        ) { query, indexed: List<IndexedContact> ->
            val visible =
                if (query.isNullOrBlank()) indexed else indexed.filter { it.matches(query) }
            AddContactUiState.Data(
                contacts = visible.map { it.ui }.toImmutableList(),
                query = query,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, AddContactUiState.Loading)
    }

    /**
     * Set the current search query.
     *
     * @param query the query text, or null to clear the search.
     */
    fun setQuery(query: String?) {
        viewModelScope.launch { queryChannel.send(query) }
    }

    /**
     * Resolve the selected contact handles to their emails.
     *
     * @param handles the handles currently selected in the UI.
     * @return the emails of the selected contacts, in no particular order.
     */
    fun emailsForSelected(handles: Set<Long>): List<String> =
        handles.mapNotNull { handleToEmail[it] }

    private fun IndexedContact.matches(query: String): Boolean {
        val q = query.lowercase()
        return ui.displayName.lowercase().contains(q)
                || data.email.lowercase().contains(q)
                || data.contactData.fullName?.lowercase()?.contains(q) == true
                || data.contactData.alias?.lowercase()?.contains(q) == true
    }

    /**
     * Indexed contact
     *
     * @property data
     * @property ui
     */
    private data class IndexedContact(
        val data: ContactItem,
        val ui: ContactItemUiState,
    )
}
