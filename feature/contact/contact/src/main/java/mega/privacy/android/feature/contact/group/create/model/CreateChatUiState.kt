package mega.privacy.android.feature.contact.group.create.model

import kotlinx.collections.immutable.ImmutableList
import mega.privacy.android.shared.contact.model.ContactItemUiState

/**
 * Create chat ui state. Backs the searchable MEGA-contacts multi-select picker shared by the
 * "create group chat" and "new chat" flows. The selection and any settings form are owned by the
 * Compose layer; this state only carries the searchable contact list.
 */
sealed interface CreateChatUiState {
    /**
     * Loading
     */
    data object Loading : CreateChatUiState

    /**
     * Data
     *
     * @property contacts MEGA contacts to display, already filtered by [query].
     * @property query Current search query, or null when not searching.
     * @property allowGroupImageSelection Feature flag value for custom chat grout image.
     */
    data class Data(
        val contacts: ImmutableList<ContactItemUiState>,
        val query: String?,
        val allowGroupImageSelection: Boolean,
    ) : CreateChatUiState {
        /**
         * Whether there are no contacts to display (no contacts at all, or none match the query).
         */
        val isEmpty: Boolean get() = contacts.isEmpty()
    }
}
