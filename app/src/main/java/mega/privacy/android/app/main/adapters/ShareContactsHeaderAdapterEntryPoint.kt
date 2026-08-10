package mega.privacy.android.app.main.adapters

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.app.main.adapters.mapper.ShareContactInfoToContactItemUiStateMapper

/**
 * Hilt entry point for [ShareContactsHeaderAdapter], a Java RecyclerView adapter
 * that cannot use constructor injection.
 *
 * Exposes the [ShareContactInfoToContactItemUiStateMapper] so the adapter can
 * build [mega.privacy.android.shared.contact.model.ContactItemUiState] values
 * for the embedded `ComposeView`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface ShareContactsHeaderAdapterEntryPoint {

    /**
     * @return the shared [ShareContactInfoToContactItemUiStateMapper].
     */
    fun shareContactInfoToContactItemUiStateMapper(): ShareContactInfoToContactItemUiStateMapper
}
