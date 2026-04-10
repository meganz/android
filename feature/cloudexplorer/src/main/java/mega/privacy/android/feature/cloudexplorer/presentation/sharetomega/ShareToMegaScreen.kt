package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.data.extensions.toUri
import mega.privacy.android.domain.entity.cloudexplorer.ExplorerMode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.cloudexplorer.presentation.explorer.ExplorerScreen
import mega.privacy.android.navigation.destination.ExplorerNavKey
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.transfers.components.UploadUrisEventState
import mega.privacy.android.shared.transfers.components.UploadingFiles
import mega.privacy.android.shared.transfers.components.rememberUploadUrisEventState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareToMegaScreen(
    uiState: ShareToMegaUiState,
    startNavKey: ExplorerNavKey,
    onStartUpload: (TransferTriggerEvent) -> Unit,
    onNavigateBack: () -> Unit,
    onNavigate: (NavKey) -> Unit,
) {
    if (uiState is ShareToMegaUiState.Loading) {
        //See if we need a loading view
    } else {
        val dataUiState = uiState as ShareToMegaUiState.Data
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

        ShareFilesToMegaUpload(
            parentNodeId = folderPickedId,
            uploadUrisEventState = uploadUrisEventState,
            onStartUpload = onStartUpload,
            onCloseExplorerScreen = onNavigateBack,
        )
    }
}

/**
 * Reusable composable that handles the upload flow when sharing files to MEGA.
 * It manages name collision resolution, over-quota handling, upload errors,
 * and triggers the actual upload with a snackbar + delayed screen close.
 *
 * @param parentNodeId The destination folder [NodeId] where files will be uploaded.
 * @param uploadUrisEventState The state holding the URIs event to upload.
 * @param onStartUpload Callback invoked with the [TransferTriggerEvent] to start the upload.
 * @param onCloseExplorerScreen Callback to close/navigate back from the current screen.
 */
@Composable
fun ShareFilesToMegaUpload(
    parentNodeId: NodeId,
    uploadUrisEventState: UploadUrisEventState,
    onStartUpload: (TransferTriggerEvent) -> Unit,
    onCloseExplorerScreen: () -> Unit,
) {
    val snackbarHostState = LocalSnackBarHostState.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    val megaResultContract = rememberMegaResultContract()

    val showMessageAndClose: (String?) -> Unit = { message ->
        coroutineScope.launch {
            message?.let {
                snackbarHostState?.showAutoDurationSnackbar(it)
            } ?: snackbarHostState?.showAutoDurationSnackbar(
                resources.getString(sharedR.string.transfers_upload_started_snackbar)
            )
            onCloseExplorerScreen()
        }
    }
    val nameCollisionLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.nameCollisionActivityContract
    ) { message ->
        if (message.isNullOrEmpty()) {
            onCloseExplorerScreen()
        } else {
            showMessageAndClose(message)
        }
    }

    UploadingFiles(
        nameCollisionLauncher = nameCollisionLauncher,
        parentNodeId = parentNodeId,
        urisEvent = uploadUrisEventState.event,
        onUrisConsumed = uploadUrisEventState::consume,
        pitagTrigger = PitagTrigger.ShareFromApp,
        onStartUpload = { transferTriggerEvent ->
            onStartUpload(transferTriggerEvent)
            showMessageAndClose(null)
        },
    )
}