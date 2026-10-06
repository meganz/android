package mega.privacy.android.shared.nodes.paging

import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.model.TypedNodeItem

/**
 * What a [NodeListPager] has loaded so far
 *
 * @property items the loaded prefix of the list, in the query's order
 * @property loadState see [PagedLoadState]
 * @property hasMore whether rows exist beyond [items]; true until the first page has answered
 * @property total the folder's child count when the pager was given one, for app-bar text only.
 * It counts every child regardless of any sensitivity filter, so it can exceed the reachable rows.
 * @property reconcileVersion bumped each time [items] was replaced by a completed re-read, so a
 * selection prunes its ids on it instead of on every append
 */
data class PagedNodes<T : TypedNode>(
    val items: List<TypedNodeItem<T>> = emptyList(),
    val loadState: PagedLoadState = PagedLoadState.FirstPage,
    val hasMore: Boolean = true,
    val total: Int? = null,
    val reconcileVersion: Int = 0,
)

/**
 * The paged load state in the terms the node list screens already read. A failed append keeps the
 * loaded items and reads as fully loaded; a failed first page reads as failed.
 */
val PagedNodes<*>.nodesLoadingState: NodesLoadingState
    get() = when (loadState) {
        PagedLoadState.FirstPage -> NodesLoadingState.Loading
        PagedLoadState.Idle, PagedLoadState.PageFailed -> NodesLoadingState.FullyLoaded
        PagedLoadState.LoadingMore -> NodesLoadingState.PartiallyLoaded
        PagedLoadState.FirstPageFailed -> NodesLoadingState.Failed
    }
