package mega.privacy.android.app.presentation.search.model

import androidx.annotation.StringRes
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.app.presentation.node.view.ToolbarMenuItem
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.MoveRequestResult
import mega.privacy.android.domain.entity.node.NodeNameCollisionResult
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * State for SearchActivity
 * @property dropdownChipsEnabled are new dropdown chips enabled via feature flag
 * @property searchDescriptionEnabled is search by description enabled via feature flag
 * @property searchItemList list of search items in [TypedNode]
 * @property isSearching to show loading or not
 * @property sortOrder [SortOrder] to display nodes
 * @property currentViewType current [ViewType]
 * @property searchQuery current typed search query in search activity
 * @property optionsItemInfo options info needed to be displayed on toolbar when any nodes are selected
 * @property errorMessageId error message id to be shown on UI
 * @property filters search filter categories
 * @property selectedFilter selected filter which is enabled on chips
 * @property typeSelectedFilterOption the type selected filter option
 * @property dateModifiedSelectedFilterOption the date modified selected filter option
 * @property dateAddedSelectedFilterOption the date added selected filter option
 * @property nodeSourceType type of Node Source
 * @property emptyState empty state to be shown on UI
 * @property toolbarMenuItems list of [ToolbarMenuItem] to be shown on toolbar
 * @property selectedNodes selected nodes
 * @property nodeNameCollisionResult result of node name collision
 * @property moveRequestResult result of move request
 * @property navigationLevel list of parent handles
 * @property resetScroll to reset scroll position
 * @property accountType account type (free/paid)
 */
data class SearchActivityState(
    val dropdownChipsEnabled: Boolean? = null,
    val searchDescriptionEnabled: Boolean? = null,
    val searchItemList: List<NodeUIItem<TypedNode>> = emptyList(),
    val isSearching: Boolean = true,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val currentViewType: ViewType = ViewType.LIST,
    val searchQuery: String = "",
    val selectedNodes: Set<TypedNode> = emptySet(),
    val optionsItemInfo: OptionsItemInfo? = null,
    @StringRes val errorMessageId: Int? = null,
    val filters: List<SearchFilter> = emptyList(),
    val selectedFilter: SearchFilter? = null,
    val typeSelectedFilterOption: TypeFilterWithName? = null,
    val dateModifiedSelectedFilterOption: DateFilterWithName? = null,
    val dateAddedSelectedFilterOption: DateFilterWithName? = null,
    val emptyState: Pair<Int, String>? = null,
    val toolbarMenuItems: List<ToolbarMenuItem> = emptyList(),
    val nodeSourceType: NodeSourceType = NodeSourceType.OTHER,
    val nodeNameCollisionResult: NodeNameCollisionResult? = null,
    val moveRequestResult: Result<MoveRequestResult>? = null,
    val navigationLevel: List<Pair<Long, String>> = emptyList(),
    val resetScroll: Boolean = false,
    val accountType: AccountType? = null,
)
