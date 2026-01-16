package mega.privacy.android.feature.clouddrive.presentation.offline.model

import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * UI state for the OfflineScreen
 * @param isLoadingCurrentFolder if true, current folder files are still loading
 * @param isLoadingChildFolders if true, current folder is still not the final folder to show
 * @param showOfflineWarning UI state to show the offline warning
 * @param offlineNodes The offline nodes fetched from the database
 * @param selectedNodeHandles The selected nodes when the view is in the selecting mode
 * @param nodeId Parent id of Node
 * @param title Title of screen
 * @param currentViewType ViewType [ViewType]
 * @param isOnline true if connected to network
 * @param searchQuery Search query
 * @param closeSearchViewEvent Event to close search view
 * @param openFolderInPageEvent Event to open folder in a new fragment
 * @param openOfflineNodeEvent Event to open offline node
 */
data class OfflineUiState(
    val isLoadingCurrentFolder: Boolean = true,
    val isLoadingChildFolders: Boolean = false,
    val showOfflineWarning: Boolean = false,
    val offlineNodes: List<OfflineNodeUiItem> = emptyList(),
    val selectedNodeHandles: List<Long> = emptyList(),
    val nodeId: Int = -1,
    val title: String? = null,
    val path: String? = null,
    val highlightedFiles: Set<String> = emptySet(),
    val currentViewType: ViewType = ViewType.LIST,
    val isOnline: Boolean = false,
    val searchQuery: String? = null,
    val openFolderInPageEvent: StateEventWithContent<OfflineFileInformation> = consumed(),
    val openOfflineNodeEvent: StateEventWithContent<OfflineFileInformation> = consumed(),
    val selectedSortOrder: SortOrder = SortOrder.ORDER_DEFAULT_ASC,
    val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
) {

    /**
     * isLoading UI state to show the loading state
     */
    val isLoading: Boolean
        get() = isLoadingCurrentFolder || isLoadingChildFolders

    /**
     * Get the selected offline nodes
     */
    val selectedOfflineNodes: List<OfflineFileInformation>
        get() = offlineNodes.filter { it.isSelected }.map { it.offlineFileInformation }

    /**
     * Check if all nodes are selected
     *
     * Works by comparing the size of the selected nodes with the size of the offline nodes
     * to make the calculation more efficient and avoid looping through all the nodes to check
     * if they are selected.
     *
     * @return true if all nodes are selected
     */
    val areAllNodesSelected: Boolean = selectedNodeHandles.size == offlineNodes.size
}
