package mega.privacy.android.feature.contact.group.create.model

import kotlinx.collections.immutable.ImmutableList
import mega.privacy.android.shared.contact.model.ContactItemUiState

/**
 * Create group chat ui state. Backs the distinct "create group chat" screen, whose first step is a
 * MEGA-contacts multi-select picker (identical to the shared picker) and whose second step is a group
 * settings form. The group settings and the selection are owned by the Compose layer; this state only
 * carries the searchable contact list.
 */
sealed interface CreateGroupChatUiState {
    /**
     * Loading
     */
    data object Loading : CreateGroupChatUiState

    /**
     * Data
     *
     * @property contacts MEGA contacts to display, already filtered by [query].
     * @property query Current search query, or null when not searching.
     */
    data class Data(
        val contacts: ImmutableList<ContactItemUiState>,
        val query: String?,
    ) : CreateGroupChatUiState {
        /**
         * Whether there are no contacts to display (no contacts at all, or none match the query).
         */
        val isEmpty: Boolean get() = contacts.isEmpty()
    }
}
