package mega.privacy.android.app.presentation.videosection.model

import mega.privacy.android.app.presentation.data.NodeUIItem
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.legacy.core.ui.model.SearchWidgetState

/**
 * Video selected state
 *
 * @property currentViewType serves as the original view type
 * @property sortOrder the sort order
 * @property isLoading true if loading
 * @property nodesList node list
 * @property currentFolderHandle the current opened folder node
 * @property openedFolderNodeHandles Set of opened folder node handles
 * @property topBarTitle top bar title
 * @property query search query
 * @property searchState SearchWidgetState
 * @property selectedNodeHandles the selected node handles
 */
data class VideoSelectedState(
    val currentViewType: ViewType = ViewType.LIST,
    val sortOrder: SortOrder = SortOrder.ORDER_NONE,
    val isLoading: Boolean = true,
    val nodesList: List<NodeUIItem<TypedNode>> = emptyList(),
    val currentFolderHandle: Long = -1,
    val openedFolderNodeHandles: Set<Long> = emptySet(),
    val topBarTitle: String? = null,
    val query: String? = null,
    val searchState: SearchWidgetState = SearchWidgetState.COLLAPSED,
    val selectedNodeHandles: List<Long> = emptyList(),
)