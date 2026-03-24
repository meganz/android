package mega.privacy.android.feature.clouddrive.presentation.clouddrive.model

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.components.banners.OverQuotaStatus
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.TypedNodeItem

/**
 * UI state for Cloud Drive
 * @param title Name of the folder
 * @property nodesLoadingState Current state of node loading
 * @property currentFolderId The current folder id being displayed
 * @property isCloudDriveRoot True if the current folder is the root of the Cloud Drive
 * @property items List of nodes in the current folder
 * @property currentViewType The current view type of the Cloud Drive
 * @property navigateToFolderEvent Event to navigate to a folder
 * @property navigateBack Event to navigate back
 * @property openedFileNode The file node that is currently opened
 * @property showHiddenNodes True if hidden nodes should be shown forcefully based on user settings
 * @property isHiddenNodesEnabled True if user is eligible for hidden nodes feature
 * @property hasMediaItems True if there are media(image, video) items in the current folder
 */
@Immutable
data class CloudDriveUiState(
    val title: LocalizedText = LocalizedText.Literal(""),
    val nodesLoadingState: NodesLoadingState = NodesLoadingState.Loading,
    val isHiddenNodeSettingsLoading: Boolean = true,
    val currentFolderId: NodeId = NodeId(-1L),
    val isCloudDriveRoot: Boolean = false,
    val items: List<TypedNodeItem<TypedNode>> = emptyList(),
    val currentViewType: ViewType = ViewType.LIST,
    val navigateToFolderEvent: StateEventWithContent<TypedNode> = consumed(),
    val navigateBack: StateEvent = consumed,
    val openedFileNode: TypedFileNode? = null,
    val showHiddenNodes: Boolean = false,
    val isHiddenNodesEnabled: Boolean = false,
    val hasMediaItems: Boolean = true,
    val selectedSortOrder: SortOrder = SortOrder.ORDER_DEFAULT_ASC,
    val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
    val overQuotaStatus: OverQuotaStatus = OverQuotaStatus(),
    val shouldShowWarning: Boolean = true,
    val isContactVerificationOn: Boolean = false,
    val showContactNotVerifiedBanner: Boolean = false,
    val nodeSourceType: NodeSourceType = NodeSourceType.CLOUD_DRIVE,
    val hasWritePermission: Boolean = false,
    val isSearchRevampEnabled: Boolean = false,
) {
    /**
     * True if nodes or hidden node settings are loading
     */
    val isLoading = nodesLoadingState == NodesLoadingState.Loading || isHiddenNodeSettingsLoading

    /**
     * Count of visible items based on hidden nodes settings
     */
    val visibleItemsCount: Int = if (showHiddenNodes || !isHiddenNodesEnabled) {
        items.size
    } else {
        items.count { !it.isSensitive }
    }

    /**
     * True if there are no visible items and not loading
     */
    val isEmpty = visibleItemsCount == 0 && !isLoading

    /**
     * True if upload is allowed in the current folder
     */
    val isUploadAllowed = hasWritePermission
            && nodeSourceType != NodeSourceType.RUBBISH_BIN

    /**
     * True if media discovery is allowed in the current folder based on source, media presence
     */
    val isMediaDiscoveryAllowed =
        nodeSourceType == NodeSourceType.CLOUD_DRIVE && hasMediaItems && !isCloudDriveRoot
}
