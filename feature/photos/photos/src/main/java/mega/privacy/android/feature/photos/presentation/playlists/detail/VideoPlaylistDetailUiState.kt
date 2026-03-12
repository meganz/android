package mega.privacy.android.feature.photos.presentation.playlists.detail

import androidx.compose.runtime.Stable
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.shared.nodes.model.NodeSortConfiguration
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.photos.presentation.playlists.detail.model.VideoPlaylistDetailSelectionMenuAction
import mega.privacy.android.feature.photos.presentation.playlists.detail.model.VideoPlaylistDetailUiEntity

@Stable
sealed interface VideoPlaylistDetailUiState {
    data object Loading : VideoPlaylistDetailUiState

    /**
     * The state is for the video playlist detail screen
     *
     * @property playlistDetail The video playlist detail ui entity
     * @property selectedTypedNodes The selected typed nodes
     * @property showHiddenItems Whether to show hidden items
     * @property isHiddenNodesEnabled Whether hidden nodes are enabled
     * @property selectedCount The count of selected items
     * @property areAllSelected Whether all items are selected
     * @property selectedElementIds Element ids of selected videos (derived from playlistDetail.videos)
     * @property selectedSortConfiguration the selected sort configuration
     */
    data class Data(
        val playlistDetail: VideoPlaylistDetailUiEntity? = null,
        val selectedTypedNodes: Set<TypedNode> = emptySet(),
        val showHiddenItems: Boolean = false,
        val isHiddenNodesEnabled: Boolean = false,
        val selectedSortConfiguration: NodeSortConfiguration = NodeSortConfiguration.default,
    ) : VideoPlaylistDetailUiState {
        val selectedElementIds: Set<Long>
            get() = playlistDetail?.videos
                ?.filter { it.id in selectedTypedNodes.map { node -> node.id } }
                ?.mapNotNull { it.elementID }
                ?.toSet() ?: emptySet()

        val selectedCount: Int
            get() = selectedTypedNodes.size

        val areAllSelected: Boolean
            get() = selectedTypedNodes.isNotEmpty() && playlistDetail?.videos?.size == selectedTypedNodes.size

        val bottomBarActions: List<MenuActionWithIcon>
            get() = if (selectedTypedNodes.isEmpty()) {
                emptyList()
            } else {
                val includeSensitiveInheritedNode =
                    selectedTypedNodes.any { it.isSensitiveInherited }
                val hasNonSensitiveNode = selectedTypedNodes.any { !it.isMarkedSensitive }
                val isNodeHidden =
                    isHiddenNodesEnabled && !hasNonSensitiveNode && !includeSensitiveInheritedNode

                val isSystemPlaylist = playlistDetail?.uiEntity?.isSystemVideoPlayer == true
                buildList {
                    if (isSystemPlaylist) {
                        add(VideoPlaylistDetailSelectionMenuAction.Download)
                        add(VideoPlaylistDetailSelectionMenuAction.SendToChat)
                        add(VideoPlaylistDetailSelectionMenuAction.Share)
                        add(VideoPlaylistDetailSelectionMenuAction.RemoveFavourite)
                    } else {
                        add(VideoPlaylistDetailSelectionMenuAction.RemoveFromPlaylist)
                    }
                    if (isNodeHidden) {
                        add(VideoPlaylistDetailSelectionMenuAction.Unhide)
                    } else {
                        add(VideoPlaylistDetailSelectionMenuAction.Hide)
                    }
                }
            }
    }
}


