package mega.privacy.android.feature.cloudexplorer.presentation.addvideotoplaylist

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerScreen
import mega.privacy.android.navigation.destination.AddVideoToPlaylistNavKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AddVideoToPlaylistScreen(
    uiState: AddVideoToPlaylistUiState,
    startNavKey: AddVideoToPlaylistNavKey,
    onVideosPicked: (videoIds: List<NodeId>) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
) {
    if (uiState is AddVideoToPlaylistUiState.Data) {
        var isProcessingAction by rememberSaveable { mutableStateOf(false) }
        val disabledNodeIds = remember(startNavKey.addedVideoIds) {
            startNavKey.addedVideoIds.toSet()
        }

        ExplorerScreen(
            explorerMode = ExplorerMode.AddVideosToPlaylist,
            startNavKey = startNavKey,
            isInnerNavigation = false,
            nodeExplorerId = uiState.rootNodeId,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            onCloseExplorerScreen = onNavigateBack,
            onNavigateBack = onNavigateBack,
            onNavigate = onNavigate,
            isProcessingAction = isProcessingAction,
            disabledNodeIds = disabledNodeIds,
            onFilesPicked = { nodeIds ->
                isProcessingAction = true
                onVideosPicked(nodeIds)
                onNavigateBack()
            },
        )
    }
}
