package mega.privacy.android.feature.contact.group.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mega.privacy.android.core.coroutine.asUiStateFlow
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.usecase.contact.GetContactsUseCase
import mega.privacy.android.feature.contact.group.create.model.CreateGroupChatUiState
import mega.privacy.android.shared.contact.mapper.ContactItemUiStateMapper
import mega.privacy.android.shared.contact.model.ContactItemUiState
import timber.log.Timber
import javax.inject.Inject

/**
 * Create group chat view model. Backs the distinct "create group chat" screen: exposes the searchable
 * MEGA-contacts list for the selection step and resolves the selected handles to emails on confirm.
 * Selection and the group-settings form (name, EKR, chat link, allow-add-participants) are owned by the
 * Compose layer, not this ViewModel. The screen builds a `NewGroupChatResult` from the selection plus the
 * settings and returns it to the caller, which performs the actual group creation.
 *
 * @property getContactsUseCase
 * @property contactItemUiStateMapper
 */
@HiltViewModel
class CreateGroupChatViewModel @Inject constructor(
    private val getContactsUseCase: GetContactsUseCase,
    private val contactItemUiStateMapper: ContactItemUiStateMapper,
) : ViewModel() {

    private val queryChannel = Channel<String?>(Channel.CONFLATED)

    /**
     * Resolves a selected contact handle back to its email. Retained from the full (unfiltered)
     * contact list so a selected contact still resolves even when it has been filtered out of the
     * visible list by the current search query.
     */
    private var handleToEmail: Map<Long, String> = emptyMap()

    /**
     * Ui state
     */
    val uiState: StateFlow<CreateGroupChatUiState> by lazy {
        combine(
            queryChannel.receiveAsFlow().onStart { emit(null) },
            getContactsUseCase().map { domainList ->
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
            CreateGroupChatUiState.Data(
                contacts = visible.map { it.ui }.toImmutableList(),
                query = query,
            )
        }.catch { Timber.e(it) }
            .asUiStateFlow(viewModelScope, CreateGroupChatUiState.Loading)
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

    private data class IndexedContact(
        val data: ContactItem,
        val ui: ContactItemUiState,
    )
}
