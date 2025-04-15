package mega.privacy.android.app.presentation.offline.offlinecompose.model

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.offline.OfflineFileInformation
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * UI state for the OfflineComposeViewModel
 * @param isLoadingCurrentFolder if true, current folder files are still loading
 * @param isLoadingChildFolders if true, current folder is still not the final folder to show
 * @param showOfflineWarning UI state to show the offline warning
 * @param offlineNodes The offline nodes fetched from the database
 * @param selectedNodeHandles The selected nodes when the view is in the selecting mode
 * @param parentId Parent id of Node
 * @param title Title of screen
 * @param defaultTitle default title
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
    val offlineNodes: List<OfflineNodeUIItem> = emptyList(),
    val selectedNodeHandles: List<Long> = emptyList(),
    val parentId: Int = -1,
    val title: String? = null,
    val defaultTitle: String = "",
    val currentViewType: ViewType = ViewType.LIST,
    val isOnline: Boolean = false,
    val searchQuery: String? = null,
    val closeSearchViewEvent: StateEvent = consumed,
    val openFolderInPageEvent: StateEventWithContent<OfflineFileInformation> = consumed(),
    val openOfflineNodeEvent: StateEventWithContent<OfflineFileInformation> = consumed(),
) {

    /**
     * isLoading UI state to show the loading state
     */
    val isLoading = isLoadingCurrentFolder || isLoadingChildFolders

    /**
     * Actual title to show, it's [title] if it's not null or blank, [defaultTitle] otherwise
     */
    val actualTitle = title?.takeIf { it.isNotBlank() } ?: defaultTitle

    /**
     * Actual subtitle to show, it's default title if [actualTitle] is not null, null otherwise
     */
    val actualSubtitle = if (title.isNullOrEmpty()) null else defaultTitle

    /**
     * Get the selected offline nodes
     */
    val selectedOfflineNodes: List<OfflineFileInformation>
        get() = offlineNodes.filter {
            it.isSelected
        }.map { it.offlineNode }
}