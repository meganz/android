package mega.privacy.android.feature.contact.add.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Compose-owned selection state for the contacts multi-select picker. MEGA contacts are keyed by the
 * stable contact handle, while phone contacts are keyed by email (they have no MEGA handle). Selection
 * is held independently of the (filterable) contact list, so a selected contact stays selected across
 * search/filter changes and reappears selected when the filter clears.
 *
 * @param initialSelectedHandles MEGA contact handles to pre-select.
 * @param initialSelectedPhoneEmails phone-contact emails to pre-select.
 */
@Stable
class ContactSelectionState(
    initialSelectedHandles: Set<Long> = emptySet(),
    initialSelectedPhoneEmails: Set<String> = emptySet(),
) {
    var selectedHandles: Set<Long> by mutableStateOf(initialSelectedHandles)
        private set

    var selectedPhoneEmails: Set<String> by mutableStateOf(initialSelectedPhoneEmails)
        private set

    /**
     * Number of currently selected items across both MEGA contacts and phone contacts.
     */
    val selectedItemsCount: Int
        get() = selectedHandles.size + selectedPhoneEmails.size

    /**
     * Toggle the selection of the MEGA contact with the given [handle].
     */
    fun toggleSelection(handle: Long) {
        selectedHandles = if (handle in selectedHandles) {
            selectedHandles - handle
        } else {
            selectedHandles + handle
        }
    }

    /**
     * Toggle the selection of the phone contact with the given [email].
     */
    fun togglePhoneSelection(email: String) {
        selectedPhoneEmails = if (email in selectedPhoneEmails) {
            selectedPhoneEmails - email
        } else {
            selectedPhoneEmails + email
        }
    }

    /**
     * Select the phone contacts with the given [emails] without deselecting any existing selection.
     */
    fun selectPhoneEmails(emails: Collection<String>) {
        selectedPhoneEmails = selectedPhoneEmails + emails
    }

    /**
     * Clear the current selection.
     */
    fun deselectAll() {
        selectedHandles = emptySet()
        selectedPhoneEmails = emptySet()
    }

    companion object {
        val Saver: Saver<ContactSelectionState, List<List<String>>> = Saver(
            save = { state ->
                listOf(
                    state.selectedHandles.map { it.toString() },
                    state.selectedPhoneEmails.toList(),
                )
            },
            restore = { saved ->
                ContactSelectionState(
                    initialSelectedHandles = saved.getOrNull(0)
                        ?.map { it.toLong() }
                        ?.toSet()
                        ?: emptySet(),
                    initialSelectedPhoneEmails = saved.getOrNull(1)?.toSet() ?: emptySet(),
                )
            },
        )
    }
}

/**
 * Remember a [ContactSelectionState] that survives recomposition and configuration changes.
 */
@Composable
fun rememberContactSelectionState(
    initialSelectedHandles: Set<Long> = emptySet(),
    initialSelectedPhoneEmails: Set<String> = emptySet(),
): ContactSelectionState =
    rememberSaveable(saver = ContactSelectionState.Saver) {
        ContactSelectionState(
            initialSelectedHandles = initialSelectedHandles,
            initialSelectedPhoneEmails = initialSelectedPhoneEmails,
        )
    }
