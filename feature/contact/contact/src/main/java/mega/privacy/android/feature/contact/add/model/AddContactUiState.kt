package mega.privacy.android.feature.contact.add.model

import de.palm.composestateevents.StateEventWithContent
import kotlinx.collections.immutable.ImmutableList
import mega.privacy.android.shared.contact.model.AvatarData
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
     * @property scannedContactDialog Dialog to show as the outcome of a QR scan, or null when none.
     * @property scannedContactSelectEvent One-shot event carrying the handle of a scanned contact
     * that is already in the loaded list so the screen can auto-select it.
     * @property scannedContactInviteEvent One-shot event carrying the outcome of inviting a
     * scanned contact so the screen can surface feedback.
     */
    data class Data(
        val contacts: ImmutableList<ContactItemUiState>,
        val query: String?,
        val showUserLimitWarning: Boolean,
        val phoneContactsSection: PhoneContactsSection,
        val phoneContactsPickedEvent: StateEventWithContent<List<String>>,
        val scannedContactDialog: ScannedContactDialog?,
        val scannedContactSelectEvent: StateEventWithContent<Long>,
        val scannedContactInviteEvent: StateEventWithContent<ScannedContactInviteFeedback>,
    ) : AddContactUiState {
        /**
         * Whether there are no contacts to display (no contacts at all, or none match the query).
         */
        val isEmpty: Boolean get() = contacts.isEmpty()
    }
}

/**
 * Mutually exclusive dialogs shown as the outcome of scanning a contact QR code.
 */
sealed interface ScannedContactDialog {
    /**
     * The scanned code is not a valid MEGA contact link, or the contact link query failed.
     */
    data object InvalidCode : ScannedContactDialog

    /**
     * The barcode scanner module is still downloading and the scan cannot start yet.
     */
    data object ScannerNotInstalled : ScannedContactDialog

    /**
     * The scanned user is already a contact but cannot be selected in this picker.
     *
     * @property email Email of the already-added contact.
     */
    data class AlreadyAdded(
        val email: String,
    ) : ScannedContactDialog

    /**
     * The scanned user was found and is not yet a contact, so they can be invited.
     *
     * @property contactName Display name of the scanned contact.
     * @property email Email of the scanned contact.
     * @property handle Handle of the scanned contact, needed to send the invitation.
     * @property avatar Avatar of the scanned contact.
     */
    data class Found(
        val contactName: String,
        val email: String,
        val handle: Long,
        val avatar: AvatarData,
    ) : ScannedContactDialog
}

/**
 * Outcome of inviting a scanned contact, surfaced to the user as one-shot feedback.
 */
enum class ScannedContactInviteFeedback {
    /**
     * The invitation was sent (or resent) successfully.
     */
    Sent,

    /**
     * The invitation could not be sent.
     */
    Failed,
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
