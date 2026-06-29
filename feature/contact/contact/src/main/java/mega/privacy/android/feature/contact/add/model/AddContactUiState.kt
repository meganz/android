package mega.privacy.android.feature.contact.add.model

import kotlinx.collections.immutable.ImmutableList
import mega.privacy.android.shared.contact.model.ContactItemUiState

/**
 * Add contact ui state
 */
sealed interface AddContactUiState {
    /**
     * Loading
     */
    data object Loading : AddContactUiState

    /**
     * Data
     *
     * @property contacts MEGA contacts to display, already filtered by [query].
     * @property query Current search query, or null when not searching.
     * @property showUserLimitWarning Whether to show the call user-limit warning (meeting flow only).
     */
    data class Data(
        val contacts: ImmutableList<ContactItemUiState>,
        val query: String?,
        val showUserLimitWarning: Boolean,
    ) : AddContactUiState {
        /**
         * Whether there are no contacts to display (no contacts at all, or none match the query).
         */
        val isEmpty: Boolean get() = contacts.isEmpty()
    }
}
