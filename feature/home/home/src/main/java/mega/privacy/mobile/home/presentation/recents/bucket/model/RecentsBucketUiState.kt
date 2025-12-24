package mega.privacy.mobile.home.presentation.recents.bucket.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.model.NodeUiItem
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.TypedNode

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
    val excludeSensitives: Boolean = false,
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
}