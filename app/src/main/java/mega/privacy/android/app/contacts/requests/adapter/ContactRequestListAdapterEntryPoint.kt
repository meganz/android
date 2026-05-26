package mega.privacy.android.app.contacts.requests.adapter

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.contacts.requests.mapper.ContactRequestItemToContactItemUiStateMapper

/**
 * Hilt entry point for [ContactRequestListAdapter], a RecyclerView adapter
 * created with a click callback rather than full constructor injection.
 *
 * Exposes the [ContactRequestItemToContactItemUiStateMapper] so the adapter
 * can build [mega.privacy.android.shared.contact.model.ContactItemUiState]
 * values for the embedded `ComposeView`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ContactRequestListAdapterEntryPoint {

    /**
     * @return the shared [ContactRequestItemToContactItemUiStateMapper].
     */
    fun contactRequestItemToContactItemUiStateMapper(): ContactRequestItemToContactItemUiStateMapper
}
