package mega.privacy.android.feature.sync.ui.megapicker

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.node.Node
import mega.privacy.android.domain.entity.node.TypedNode

internal data class MegaPickerState(
    val currentFolder: Node? = null,
    val nodes: List<TypedNode>? = null,
    val showAllFilesAccessDialog: Boolean = false,
    val showDisableBatteryOptimizationsDialog: Boolean = false,
    val navigateNextEvent: StateEvent = consumed,
)
