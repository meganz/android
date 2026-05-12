package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.files

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerScreen
import mega.privacy.android.feature.cloudexplorer.presentation.sharetomega.ShareToMegaUpload
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.shared.transfers.components.rememberUploadUrisEventState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareFilesToMegaScreen(
    uiState: ShareFilesToMegaUiState,
    startNavKey: ExplorerNavKey,
    onStartUpload: (TransferTriggerEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
) {
    if (uiState is ShareFilesToMegaUiState.Loading) {
        //See if we need a loading view
    } else {
        val dataUiState = uiState as ShareFilesToMegaUiState.Data
        val uploadUrisEventState = rememberUploadUrisEventState()
        var folderPickedIdLong by rememberSaveable { mutableLongStateOf(-1L) }
        val folderPickedId = NodeId(folderPickedIdLong)

        ExplorerScreen(
            explorerMode = ExplorerMode.ShareFilesToMega,
            startNavKey = startNavKey,
            isInnerNavigation = false,
            nodeExplorerId = dataUiState.rootNodeId,
            nodeSourceType = NodeSourceType.CLOUD_DRIVE,
            shareUris = dataUiState.shareUris,
            onCloseExplorerScreen = onNavigateBack,
            onNavigateBack = onNavigateBack,
            onNavigate = onNavigate,
            onFolderPicked = { nodeId ->
                folderPickedIdLong = nodeId.longValue
                uploadUrisEventState.trigger(dataUiState.shareUris.map { it.toUri() })
            },
        )

        ShareToMegaUpload(
            parentNodeId = folderPickedId,
            uploadUrisEventState = uploadUrisEventState,
            onStartUpload = onStartUpload,
            onCloseExplorerScreen = onNavigateBack,
        )
    }
}