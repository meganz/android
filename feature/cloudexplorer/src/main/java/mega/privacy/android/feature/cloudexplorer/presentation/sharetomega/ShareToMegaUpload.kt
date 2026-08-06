package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.ui.platform.LocalResources
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.launch
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.extensions.showAutoDurationSnackbar
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.rememberOnSharedToChats
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.android.shared.transfers.components.UploadUrisEventState
import mega.privacy.android.shared.transfers.components.UploadingFiles
import mega.privacy.android.shared.transfers.model.UploadFileViewModel

/**
 * Drives the upload flow for the Share-to-MEGA screens: handles name collisions,
 * over-quota and upload errors, and forks between a cloud upload and a chat upload
 * depending on whether [chatIds] is non-empty.
 *
 * @param parentNodeId Destination folder for the cloud-upload branch.
 * @param chatIds When non-empty, switches to the chat-upload branch and the files are
 * attached to those chats; once the upload starts the screen finishes via
 * [rememberOnSharedToChats] (navigate to the chat if there is exactly one, otherwise
 * show the "Sent as message" snackbar).
 * @param viewModel The upload ViewModel; hosts pass their own instance and observe
 * `isProcessing` from its ui state to disable their upload UI while a triggered
 * upload is being processed. It resets to false when processing fails.
 */
@Composable
internal fun ShareToMegaUpload(
    parentNodeId: NodeId,
    pitagTrigger: PitagTrigger,
    uploadUrisEventState: UploadUrisEventState,
    onStartUpload: (TransferTriggerEvent) -> Unit,
    onCloseExplorerScreen: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    chatIds: List<Long>? = null,
    viewModel: UploadFileViewModel = hiltViewModel(),
) {
    val snackbarHostState = LocalSnackBarHostState.current
    val resources = LocalResources.current
    val coroutineScope = rememberCoroutineScope()
    val megaResultContract = rememberMegaResultContract()
    val onSharedToChats = rememberOnSharedToChats(
        onNavigate = onNavigate,
        onCloseExplorerScreen = onCloseExplorerScreen,
    )

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
        chatIds = chatIds,
        viewModel = viewModel,
        urisEvent = uploadUrisEventState.event,
        onUrisConsumed = uploadUrisEventState::consume,
        pitagTrigger = pitagTrigger,
        onStartUpload = { transferTriggerEvent ->
            onStartUpload(transferTriggerEvent)

            if (chatIds.isNullOrEmpty()) {
                showMessageAndClose(null)
            } else {
                onSharedToChats(chatIds)
            }
        },
    )
}