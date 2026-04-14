package mega.privacy.android.feature.sync.ui.megapicker

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.Node

internal data class MegaPickerState(
    val currentFolder: Node? = null,
    val nodes: List<TypedNodeUiModel>? = null,
    val showAllFilesAccessDialog: Boolean = false,
    val showDisableBatteryOptimizationsDialog: Boolean = false,
    val snackbarMessage: LocalizedText? = null,
    val navigateNextEvent: StateEvent = consumed,
    val isSelectEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val showRemoveConnectionDialog: Boolean = false,
    val selectedDisabledFolder: TypedNodeUiModel? = null,
)
