package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalResources
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.transfers.components.UploadUrisEventState
import mega.privacy.android.shared.transfers.components.UploadingFiles

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
internal fun ShareToMegaUpload(
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