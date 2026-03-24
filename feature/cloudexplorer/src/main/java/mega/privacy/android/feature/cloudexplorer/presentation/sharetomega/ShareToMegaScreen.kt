package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.model.ExplorerModeData
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareToMegaScreen(
    uiState: ShareToMegaUiState,
    onUpload: (NodeId) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigateToFolder: (NavKey) -> Unit,
) {
    if (uiState is ShareToMegaUiState.Loading) {
        //See if we need a loading view
    } else {
        val dataUiState = uiState as ShareToMegaUiState.Data

        ExplorerScreen(
            explorerModeData = ExplorerModeData.ShareFilesToMega,
            hideTabs = false,
            nodeExplorerId = dataUiState.rootNodeId,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            onNavigateBack = onNavigateBack,
            onNavigateToFolder = onNavigateToFolder,
            onFolderPicked = onUpload,
        )
    }
}