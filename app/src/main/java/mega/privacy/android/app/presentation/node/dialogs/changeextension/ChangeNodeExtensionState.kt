package mega.privacy.android.app.presentation.node.dialogs.changeextension

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed

internal data class ChangeNodeExtensionState(
    val renameSuccessfulEvent: StateEvent = consumed,
)