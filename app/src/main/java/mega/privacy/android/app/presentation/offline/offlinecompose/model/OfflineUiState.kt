package mega.privacy.android.app.presentation.offline.offlinecompose.model

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
 */
data class OfflineUiState(
    val isLoading: Boolean = false,
    val showOfflineWarning: Boolean = false,
    val offlineNodes: List<OfflineNodeUIItem> = emptyList(),
    val selectedNodeHandles: List<Long> = emptyList(),
    val parentId: Int = -1,
    val title: String = "",
    val currentViewType: ViewType = ViewType.LIST,
)