package mega.privacy.android.feature.contact.add.model

import de.palm.composestateevents.StateEventWithContent
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
     * @property phoneContactsSection State of the collapsible phone-contacts section.
     * @property phoneContactsPickedEvent One-shot event carrying the emails newly added by the system
     * picker so the screen can auto-select them. Only fired on the post-17 picker path.
     */
    data class Data(
        val contacts: ImmutableList<ContactItemUiState>,
        val query: String?,
        val showUserLimitWarning: Boolean,
        val phoneContactsSection: PhoneContactsSection,
        val phoneContactsPickedEvent: StateEventWithContent<List<String>>,
    ) : AddContactUiState {
        /**
         * Whether there are no contacts to display (no contacts at all, or none match the query).
         */
        val isEmpty: Boolean get() = contacts.isEmpty()
    }
}

/**
 * State of the collapsible "Phone contacts" section shown above the MEGA contact list.
 */
sealed interface PhoneContactsSection {
    /**
     * The section is not shown at all (e.g. this flow does not surface phone contacts).
     */
    data object Hidden : PhoneContactsSection

    /**
     * READ_CONTACTS permission is required before phone contacts can be listed (pre-17 path).
     */
    data object PermissionRequired : PhoneContactsSection

    /**
     * Phone contacts have been bulk-loaded and are ready to display (pre-17 path).
     *
     * @property contacts the emailable phone contacts, filtered by the current query.
     */
    data class Loaded(
        val contacts: ImmutableList<ContactItemUiState>,
    ) : PhoneContactsSection

    /**
     * The system contact picker is available (post-17 path). Contacts appear here only after the
     * user picks them.
     *
     * @property picked the contacts picked so far, filtered by the current query.
     */
    data class PickerAvailable(
        val picked: ImmutableList<ContactItemUiState>,
    ) : PhoneContactsSection
}
