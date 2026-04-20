package mega.privacy.android.navigation.contract.suppression

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the current overlay suppression state.
 *
 * Written to by [OverlaySuppressionNavEntryDecorator] (inside NavDisplay) and
 * read by the event processing layer (outside NavDisplay) to decide whether
 * suppressable events should be deferred.
 */
class OverlaySuppressionState {

    /**
     * `true` when the currently active screen declares overlay suppression.
     */
    val isSuppressing: StateFlow<Boolean>
        field = MutableStateFlow(false)

    /**
     * Update the suppression state. Called by the decorator when entries
     * enter or leave composition.
     */
    fun setSuppressing(suppressing: Boolean) {
        isSuppressing.value = suppressing
    }
}
