package mega.privacy.android.app.presentation.node.dialogs.deletenode

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed

/**
 * State for MoveToRubbishOrDeleteNodeViewModel
 * @property deleteEvent [StateEvent]
 */
data class MoveToRubbishOrDeleteNodesState(
    val deleteEvent: StateEventWithContent<String> = consumed()
)