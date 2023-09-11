package mega.privacy.android.app.presentation.offline.offlinev2.model

import mega.privacy.android.domain.entity.offline.OfflineNodeInformation

/**
 * UI state for the OfflineViewModel
 * @param isLoading UI state to show the loading state
 * @param offlineNodes The offline nodes fetched from the database
 * @param selectedNodeHandles The selected nodes when the view is in the selecting mode
 *
 *
 */
data class OfflineUIState(
    val isLoading: Boolean = false,
    val offlineNodes: List<OfflineNodeUIItem<OfflineNodeInformation>> = emptyList(),
    val selectedNodeHandles: List<String> = emptyList(),
)