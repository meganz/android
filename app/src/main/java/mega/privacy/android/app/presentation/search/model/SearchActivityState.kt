package mega.privacy.android.app.presentation.search.model

import androidx.annotation.StringRes
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.SearchType

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
 * @property searchType
 * @property emptyState
 * @property selectedNodes
 * @property lastSelectedNode
 * @property menuActions
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
    val searchType: SearchType = SearchType.OTHER,
    val menuActions: List<MenuAction> = emptyList(),
)