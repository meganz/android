package mega.privacy.android.feature.cloudexplorer.presentation.explorer

import androidx.compose.runtime.Stable
import mega.privacy.android.domain.entity.node.NodeId

@Stable
internal data class ExplorerPickerRestrictions(
    val restrictedNodeIds: Set<NodeId> = emptySet(),
    val isPickEnabled: Boolean = false,
    val onRestrictedNodeClick: (NodeId) -> Unit = {},
)
