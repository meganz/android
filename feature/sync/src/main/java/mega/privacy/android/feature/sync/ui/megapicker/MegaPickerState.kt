package mega.privacy.android.feature.sync.ui.megapicker

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.node.Node

internal data class MegaPickerState(
    val currentFolder: Node? = null,
    val nodes: List<TypedNodeUiModel>? = null,
    val showAllFilesAccessDialog: Boolean = false,
    val showDisableBatteryOptimizationsDialog: Boolean = false,
    val errorMessageId: Int? = null,
    val navigateNextEvent: StateEvent = consumed,
    val isSelectEnabled: Boolean = false,
)
