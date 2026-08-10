package mega.privacy.android.feature.photos.presentation.playlists.videoselect

import androidx.compose.runtime.Stable
import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.consumed
import mega.android.core.ui.model.LocalizedText
import mega.privacy.android.domain.entity.node.NodesLoadingState
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.feature.photos.presentation.playlists.videoselect.model.SelectVideoItemUiEntity
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration

@Stable
sealed interface SelectVideoSearchUiState {
    data object Loading : SelectVideoSearchUiState

    /**
     * Loaded state for searching and selecting videos to add to a playlist.
     *
     * @property title Title shown in the top app bar for the current folder context.
     * @property searchText Current text in the search field (may differ from [searchedQuery] while typing).
     * @property searchedQuery Query string used for the active search / result filtering.
     * @property isCloudDriveRoot Whether the search scope is the cloud drive root folder.
     * @property currentViewType Whether results are shown as list or grid.
     * @property items Nodes returned for the current query, with selection flags applied.
     * @property navigateBack Event to trigger navigation back action.
     * @property nodesLoadingState Pagination / loading state for search results.
     * @property selectedSortConfiguration Sort order applied to the result list.
     * @property showHiddenItems When true, sensitive nodes are included in [items].
     * @property selectItemHandles Handles of videos currently selected for add-to-playlist.
     * @property placeholderText Localized hint for the search input.
     * @property recentSearches Recent search strings shown when the query is empty.
     * @property isRecentSearchesLoading Whether recent searches are still being loaded.
     * @property areAllSelected True when every selectable item in [items] is in [selectItemHandles].
     */
    data class Data(
        val title: LocalizedText = LocalizedText.Literal(""),
        val searchText: String = "",
        val searchedQuery: String = "",
        val isCloudDriveRoot: Boolean = false,
        val currentViewType: ViewType = ViewType.LIST,
        val items: List<SelectVideoItemUiEntity> = emptyList(),
        val navigateBack: StateEvent = consumed,
        val nodesLoadingState: NodesLoadingState = NodesLoadingState.Loading,
        val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
        val showHiddenItems: Boolean = false,
        val selectItemHandles: Set<Long> = emptySet(),
        val placeholderText: LocalizedText = LocalizedText.Literal(""),
        val recentSearches: List<String> = emptyList(),
        val isRecentSearchesLoading: Boolean = true,
    ) : SelectVideoSearchUiState {

        val areAllSelected: Boolean
            get() = selectItemHandles.isNotEmpty() && selectItemHandles.size == items.count { it.isSelectable }
    }
}