package mega.privacy.android.app.presentation.node.dialogs.removelink

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * Data class for remove link state
 * @property removeLinkEvent [StateEventWithContent]
 */
data class RemoveNodeLinkState(
    val removeLinkEvent: StateEventWithContent<String> = consumed()
)