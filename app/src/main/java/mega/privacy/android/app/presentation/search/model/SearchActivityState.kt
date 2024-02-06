package mega.privacy.android.app.presentation.search.model

import androidx.annotation.StringRes
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.node.NodeSourceType

/**
 * State for SearchActivity
 * @property searchItemList list of search items in [TypedNode]
 * @property isSearching to show loading or not
 * @property sortOrder [SortOrder] to display nodes
 * @property currentViewType current [ViewType]
 * @property searchQuery current typed search query in search activity
 * @property optionsItemInfo options info needed to be displayed on toolbar when any nodes are selected
 * @property errorMessageId error message id to be shown on UI
 * @property filters search filter categories
 * @property selectedFilter selected filter which is enabled on chips
 * @property nodeSourceType type of Node Source
 * @property emptyState empty state to be shown on UI
 * @property selectedNodes selected nodes
 * @property lastSelectedNode last selected node
 * @property nodeNameCollisionResult result of node name collision
 * @property moveRequestResult result of move request
 */
data class SearchActivityState(
    val searchItemList: List<NodeUIItem<TypedNode>> = emptyList(),
    val isSearching: Boolean = false,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val currentViewType: ViewType = ViewType.LIST,
    val searchQuery: String = "",
    val selectedNodes: Set<TypedNode> = emptySet(),
    val lastSelectedNode: TypedNode? = null,
    val optionsItemInfo: OptionsItemInfo? = null,
    @StringRes val errorMessageId: Int? = null,
    val filters: List<SearchFilter> = emptyList(),
    val selectedFilter: SearchFilter? = null,
    val emptyState: Pair<Int, String>? = null,
    val nodeSourceType: NodeSourceType = NodeSourceType.OTHER,
    val nodeNameCollisionResult: NodeNameCollisionResult? = null,
    val moveRequestResult: Result<MoveRequestResult>? = null,
)