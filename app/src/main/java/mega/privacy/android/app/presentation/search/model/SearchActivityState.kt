package mega.privacy.android.app.presentation.search.model

import androidx.annotation.StringRes
import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.app.presentation.mapper.OptionsItemInfo
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.entity.search.SearchType

/**
 * State for SearchActivity
 * @property searchItemList list of search items in [TypedNode]
 * @property parentHandle Handle of search parent handle where query needed to be searched
 * @property isSearching to show loading or not
 * @property sortOrder [SortOrder] to display nodes
 * @property currentViewType current [ViewType]
 * @property searchQuery current typed search query in search activity
 * @property selectedFileNodes total selected file count
 * @property selectedFolderNodes total selected folder count
 * @property isInSelection to check if nodes some nodes are selected already or not
 * @property itemIndex item index of current clicked item
 * @property currentFileNode current file which is clicked
 * @property selectedNodeHandles list of node handled which are selected
 * @property optionsItemInfo options info needed to be displayed on toolbar when any nodes are selected
 * @property currentFolderClickedHandle current handle of folder clicked
 * @property errorMessageId error message id to be shown on UI
 * @property filters search filter categories
 * @property selectedFilter selected filter which is enabled on chips
 * @property searchType
 * @property emptyState
 */
data class SearchActivityState(
    val searchItemList: List<NodeUIItem<TypedNode>> = emptyList(),
    val parentHandle: Long = -1,
    val isSearching: Boolean = false,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val currentViewType: ViewType = ViewType.LIST,
    val searchQuery: String = "",
    val selectedFileNodes: Int = 0,
    val selectedFolderNodes: Int = 0,
    val isInSelection: Boolean = false,
    val itemIndex: Int = -1,
    val currentFileNode: FileNode? = null,
    val selectedNodeHandles: List<Long> = emptyList(),
    val optionsItemInfo: OptionsItemInfo? = null,
    val currentFolderClickedHandle: Long? = null,
    @StringRes val errorMessageId: Int? = null,
    val filters: List<SearchFilter> = emptyList(),
    val selectedFilter: SearchFilter? = null,
    val emptyState: Pair<Int, String>? = null,
    val searchType: SearchType = SearchType.OTHER,
)