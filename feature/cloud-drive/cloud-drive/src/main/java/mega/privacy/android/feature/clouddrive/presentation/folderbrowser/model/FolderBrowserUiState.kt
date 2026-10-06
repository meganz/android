package mega.privacy.android.feature.clouddrive.presentation.folderbrowser.model

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.TypedNodeItem
import mega.privacy.android.shared.nodes.paging.PagedNodes
import mega.privacy.android.shared.nodes.paging.nodesLoadingState

/**
 * State of the folder browser screen
 *
 * @property title
 * @property nodeSourceType
 * @property isCloudDriveRoot whether the folder is the Cloud Drive root
 * @property currentFolderId the resolved folder; null until the root has been resolved
 * @property currentViewType
 * @property paged what the pager has loaded so far
 * @property hasMediaItems whether the folder contains an image or video
 * @property selectedSortOrder
 * @property selectedSortConfiguration
 * @property showContactNotVerifiedBanner
 * @property hasWritePermission
 * @property isNodeInBackups
 * @property navigateBack
 */
@Immutable
data class FolderBrowserUiState(
    val title: LocalizedText,
    val nodeSourceType: NodeSourceType,
    val isCloudDriveRoot: Boolean,
    val currentFolderId: NodeId? = null,
    val currentViewType: ViewType = ViewType.LIST,
    val paged: PagedNodes<TypedNode> = PagedNodes(),
    val hasMediaItems: Boolean = false,
    val selectedSortOrder: SortOrder = SortOrder.ORDER_DEFAULT_ASC,
    val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
    val showContactNotVerifiedBanner: Boolean = false,
    val hasWritePermission: Boolean = false,
    val isNodeInBackups: Boolean = false,
    val navigateBack: StateEvent = consumed,
) {
    val items: List<TypedNodeItem<TypedNode>>
        get() = paged.items

    val nodesLoadingState: NodesLoadingState
        get() = paged.nodesLoadingState

    val hasMore: Boolean
        get() = paged.hasMore

    /**
     * The folder's child count for the app bar; null when unknown or filtered
     */
    val total: Int?
        get() = paged.total

    val reconcileVersion: Int
        get() = paged.reconcileVersion

    /**
     * True until the first page has landed
     */
    val isLoading: Boolean
        get() = nodesLoadingState == NodesLoadingState.Loading

    val isUploadAllowed: Boolean
        get() = hasWritePermission && nodeSourceType != NodeSourceType.RUBBISH_BIN && !isNodeInBackups

    val isMediaDiscoveryAllowed: Boolean
        get() = nodeSourceType == NodeSourceType.CLOUD_DRIVE && hasMediaItems && !isCloudDriveRoot && !isNodeInBackups
}
