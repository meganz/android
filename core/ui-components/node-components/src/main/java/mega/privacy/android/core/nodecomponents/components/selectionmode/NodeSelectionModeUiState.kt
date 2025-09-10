package mega.privacy.android.core.nodecomponents.components.selectionmode

import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction

/**
 * State for toolbar
 * @property visibleActions List of [NodeSelectionAction]
 */
data class NodeSelectionModeUiState(
    val visibleActions: List<NodeSelectionAction> = emptyList(),
)