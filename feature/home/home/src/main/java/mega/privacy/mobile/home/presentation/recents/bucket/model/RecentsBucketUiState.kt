package mega.privacy.mobile.home.presentation.recents.bucket.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.nodes.model.NodeUiItem

/**
 * UI state for RecentsBucketScreen
 *
 * @param items list of node UI items in the bucket
 * @param isLoading true if loading
 * @param fileCount number of files in the bucket
 * @param timestamp timestamp of the bucket
 * @param parentFolderName localized name of the parent folder
 * @param parentFolderHandle handle of the parent folder
 * @param nodeSourceType source type of the nodes
 * @param excludeSensitives whether sensitive items are excluded
 * @param isHiddenNodesEnabled whether the hidden nodes feature is enabled
 * @param showHiddenNodes whether to show hidden (sensitive) nodes
 * @param navigateBack event to navigate back
 */
data class RecentsBucketUiState(
    val items: List<NodeUiItem<TypedNode>> = emptyList(),
    val isMediaBucket: Boolean = false,
    val isLoading: Boolean = true,
    val fileCount: Int = 0,
    val timestamp: Long = 0L,
    val parentFolderName: LocalizedText = LocalizedText.Literal(""),
    val parentFolderHandle: Long = -1L,
    val nodeSourceType: NodeSourceType,
    val isHiddenNodesEnabled: Boolean = false,
    val showHiddenNodes: Boolean = false,
    val navigateBack: StateEvent = consumed,
) {

    /**
     * True if there are no visible items and not loading
     */
    val isEmpty = items.isEmpty() && !isLoading


    /**
     * Count of visible selected items
     */
    val selectedItemsCount: Int = items.count { it.isSelected }

    /**
     * True if any item is selected
     */
    val isInSelectionMode = selectedItemsCount > 0

    /**
     * True if all items are selected
     */
    val isAllSelected = selectedItemsCount == items.size

    /**
     * Returns a list of selected nodes.
     */
    val selectedNodes: List<TypedNode>
        get() = items.mapNotNull { if (it.isSelected) it.node else null }

    /**
     * Returns a list of current node ids in the bucket.
     */
    val nodeIds: List<Long>
        get() = items.map { it.node.id.longValue }

    /**
     * Whether sensitive items are excluded from the bucket.
     */
    val excludeSensitives: Boolean = isHiddenNodesEnabled && !showHiddenNodes

}