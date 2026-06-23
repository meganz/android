package mega.privacy.android.feature.clouddrive.presentation.clouddrive.model

import androidx.compose.runtime.Immutable
import de.palm.composestateevents.StateEvent
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.shared.nodes.model.TypedNodeItem

/**
 * Cloud drive ui state
 *
 * @property title
 * @property nodeSourceType
 * @property currentViewType
 * @property isMediaDiscoveryAllowed
 */
@Immutable
sealed interface CloudDriveUiState {
    val title: LocalizedText
    val nodeSourceType: NodeSourceType
    val currentViewType: ViewType
    val isMediaDiscoveryAllowed: Boolean

    /**
     * Loading
     *
     * @property title
     * @property nodeSourceType
     * @property currentViewType
     */
    data class Loading(
        override val title: LocalizedText,
        override val nodeSourceType: NodeSourceType,
        override val currentViewType: ViewType,
    ) : CloudDriveUiState{
        override val isMediaDiscoveryAllowed: Boolean = false
    }

    /**
     * Data
     *
     * @property title
     * @property nodesLoadingState
     * @property currentFolderId
     * @property isCloudDriveRoot
     * @property items
     * @property currentViewType
     * @property navigateBack
     * @property hasMediaItems
     * @property selectedSortOrder
     * @property selectedSortConfiguration
     * @property showContactNotVerifiedBanner
     * @property nodeSourceType
     * @property hasWritePermission
     * @property inactivityMonths
     * @property purgeTimestamp
     */
    data class Data(
        override val title: LocalizedText,
        val nodesLoadingState: NodesLoadingState,
        val currentFolderId: NodeId,
        val isCloudDriveRoot: Boolean,
        val items: List<TypedNodeItem<TypedNode>>,
        override val currentViewType: ViewType,
        val navigateBack: StateEvent,
        val hasMediaItems: Boolean,
        val selectedSortOrder: SortOrder,
        val selectedSortConfiguration: NodeSortConfiguration,
        val showContactNotVerifiedBanner: Boolean,
        override val nodeSourceType: NodeSourceType,
        val hasWritePermission: Boolean,
        val inactivityMonths: Int? = null,
        val purgeTimestamp: Long? = null,
    ) : CloudDriveUiState {

        /**
         * Flag to determine if inactivity banner should be shown. It is shown whenever an inactive
         * purge event has been received (regardless of the computed months, which may be 0).
         */
        val showInactivityBanner = inactivityMonths != null

        /**
         * True if upload is allowed in the current folder
         */
        val isUploadAllowed = hasWritePermission
                && nodeSourceType != NodeSourceType.RUBBISH_BIN

        /**
         * True if media discovery is allowed in the current folder based on source, media presence
         */
        override val isMediaDiscoveryAllowed =
            nodeSourceType == NodeSourceType.CLOUD_DRIVE && hasMediaItems && !isCloudDriveRoot
    }
}
