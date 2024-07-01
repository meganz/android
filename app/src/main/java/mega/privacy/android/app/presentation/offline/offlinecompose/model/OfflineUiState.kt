package mega.privacy.android.app.presentation.offline.offlinecompose.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * UI state for the OfflineComposeViewModel
 * @param isLoading UI state to show the loading state
 * @param showOfflineWarning UI state to show the offline warning
 * @param offlineNodes The offline nodes fetched from the database
 * @param selectedNodeHandles The selected nodes when the view is in the selecting mode
 * @param parentId Parent id of Node
 * @param title Title of screen
 * @param currentViewType ViewType [ViewType]
 * @param isOnline true if connected to network
 * @param searchQuery Search query
 * @param closeSearchViewEvent Event to close search view
 * @param openFolderInPageEvent Event to open folder in a new fragment
 * @param openOfflineNodeEvent Event to open offline node
 */
data class OfflineUiState(
    val isLoading: Boolean = true,
    val showOfflineWarning: Boolean = false,
    val offlineNodes: List<OfflineNodeUIItem> = emptyList(),
    val selectedNodeHandles: List<Long> = emptyList(),
    val parentId: Int = -1,
    val title: String = "",
    val currentViewType: ViewType = ViewType.LIST,
    val isOnline: Boolean = false,
    val searchQuery: String? = null,
    val closeSearchViewEvent: StateEvent = consumed,
    val openFolderInPageEvent: StateEventWithContent<OfflineFileInformation> = consumed(),
    val openOfflineNodeEvent: StateEventWithContent<OfflineFileInformation> = consumed()
) {
    /**
     * Get the selected offline nodes
     */
    val selectedOfflineNodes: List<OfflineFileInformation>
        get() = offlineNodes.filter {
            it.isSelected
        }.map { it.offlineNode }
}