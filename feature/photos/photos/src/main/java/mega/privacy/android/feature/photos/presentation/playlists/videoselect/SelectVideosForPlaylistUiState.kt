package mega.privacy.android.feature.photos.presentation.playlists.videoselect

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.core.nodecomponents.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.model.SelectVideoItemUiEntity

@Stable
sealed interface SelectVideosForPlaylistUiState {
    data object Loading : SelectVideosForPlaylistUiState

    /**
     * The state is for selecting videos to add to a playlist
     *
     * @property title The title of the current folder or screen
     * @property isCloudDriveRoot Whether the current folder is the root of the cloud drive
     * @property currentViewType The current view type (list or grid)
     * @property items The list of video items available for selection
     * @property navigateBack Event to trigger navigation back action
     * @property nodesLoadingState The current state of node loading
     * @property selectedSortConfiguration The selected sort configuration for the items
     * @property showHiddenItems Whether hidden items are shown
     * @property query The current search query, if any
     * @property selectItemHandles The list of selected item handles
     */
    data class Data(
        val title: LocalizedText = LocalizedText.Literal(""),
        val isCloudDriveRoot: Boolean = false,
        val currentViewType: ViewType = ViewType.LIST,
        val items: List<SelectVideoItemUiEntity> = emptyList(),
        val navigateBack: StateEvent = consumed,
        val nodesLoadingState: NodesLoadingState = NodesLoadingState.Loading,
        val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
        val showHiddenItems: Boolean = false,
        val query: String? = null,
        val selectItemHandles: Set<Long> = emptySet(),
    ) : SelectVideosForPlaylistUiState
}
