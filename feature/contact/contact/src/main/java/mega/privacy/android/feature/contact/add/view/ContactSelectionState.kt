package mega.privacy.android.feature.contact.add.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Compose-owned selection state for the contacts multi-select picker. Selection is keyed by the
 * stable contact handle and held independently of the (filterable) contact list, so a selected
 * contact stays selected across search/filter changes and reappears selected when the filter clears.
 *
 * @param initialSelectedHandles handles to pre-select.
 */
@Stable
class ContactSelectionState(
    initialSelectedHandles: Set<Long> = emptySet(),
) {
    var selectedHandles: Set<Long> by mutableStateOf(initialSelectedHandles)
        private set

    /**
     * Number of currently selected contacts.
     */
    val selectedItemsCount: Int
        get() = selectedHandles.size

    /**
     * Toggle the selection of the contact with the given [handle].
     */
    fun toggleSelection(handle: Long) {
        selectedHandles = if (handle in selectedHandles) {
            selectedHandles - handle
        } else {
            selectedHandles + handle
        }
    }

    /**
     * Clear the current selection.
     */
    fun deselectAll() {
        selectedHandles = emptySet()
    }

    companion object {
        val Saver: Saver<ContactSelectionState, List<Long>> = Saver(
            save = { state -> state.selectedHandles.toList() },
            restore = { handles -> ContactSelectionState(initialSelectedHandles = handles.toSet()) },
        )
    }
}

/**
 * Remember a [ContactSelectionState] that survives recomposition and configuration changes.
 */
@Composable
fun rememberContactSelectionState(
    initialSelectedHandles: Set<Long> = emptySet(),
): ContactSelectionState =
    rememberSaveable(saver = ContactSelectionState.Saver) {
        ContactSelectionState(initialSelectedHandles = initialSelectedHandles)
    }
