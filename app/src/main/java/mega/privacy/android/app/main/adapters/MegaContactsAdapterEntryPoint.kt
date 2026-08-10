package mega.privacy.android.app.main.adapters

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.main.adapters.mapper.MegaContactAdapterToContactItemUiStateMapper

/**
 * Hilt entry point for [MegaContactsAdapter], a RecyclerView adapter
 * that cannot use constructor injection.
 *
 * Exposes the [MegaContactAdapterToContactItemUiStateMapper] so the adapter can
 * build [mega.privacy.android.shared.contact.model.ContactItemUiState] values
 * for the embedded `ComposeView`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MegaContactsAdapterEntryPoint {

    /**
     * @return the shared [MegaContactAdapterToContactItemUiStateMapper].
     */
    fun megaContactAdapterToContactItemUiStateMapper(): MegaContactAdapterToContactItemUiStateMapper
}
