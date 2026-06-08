package mega.privacy.mobile.home.presentation.continuewhereleftoff

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.continuewhereleftoff.ContinueWhereLeftOffItem
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration

internal data class ContinueWhereLeftOffListUiState(
    val items: List<ContinueWhereLeftOffItem> = emptyList(),
    val isLoading: Boolean = true,
    val isConnected: Boolean = true,
    val openNodeEvent: StateEventWithContent<TypedFileNode> = consumed(),
    val sortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
    val currentViewType: ViewType = ViewType.LIST,
    val showSortSheet: Boolean = false,
    val showOptionsSheet: Boolean = false,
)
