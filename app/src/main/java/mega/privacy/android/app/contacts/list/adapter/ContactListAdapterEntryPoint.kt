package mega.privacy.android.app.contacts.list.adapter

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.contacts.list.mapper.ContactItemDataToContactItemUiStateMapper

/**
 * Hilt entry point for [ContactListAdapter], a RecyclerView adapter that is
 * constructed manually from a Fragment and cannot use constructor injection.
 *
 * Exposes the [ContactItemDataToContactItemUiStateMapper] so the adapter can
 * build [mega.privacy.android.shared.contact.model.ContactItemUiState] values
 * for the embedded `ComposeView`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ContactListAdapterEntryPoint {

    /**
     * @return the [ContactItemDataToContactItemUiStateMapper].
     */
    fun contactItemDataToContactItemUiStateMapper(): ContactItemDataToContactItemUiStateMapper
}
