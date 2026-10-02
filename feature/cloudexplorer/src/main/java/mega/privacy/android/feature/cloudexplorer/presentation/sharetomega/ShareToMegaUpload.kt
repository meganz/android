package mega.privacy.android.feature.cloudexplorer.presentation.sharetomega

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation3.runtime.NavKey
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.pitag.PitagTrigger
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.cloudexplorer.presentation.chatexplorer.rememberOnSharedToChats
import mega.privacy.android.shared.nodes.namecollision.rememberNameCollisionLauncher
import mega.privacy.android.shared.transfers.components.UploadUrisEventState
import mega.privacy.android.shared.transfers.components.UploadingFiles
import mega.privacy.android.shared.transfers.model.UploadFileViewModel

/**
 * Drives the upload flow for the Share-to-MEGA screens: handles name collisions,
 * over-quota and upload errors, and forks between a cloud upload and a chat upload
 * depending on whether [chatIds] is non-empty.
 *
 * Colliding files are handed to the name-collision screen and the explorer is closed first, so
 * the user lands where the explorer would have returned once the collisions are resolved.
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
    val nameCollisionLauncher = rememberNameCollisionLauncher(navigate = onNavigate)
    val onSharedToChats = rememberOnSharedToChats(
        onNavigate = onNavigate,
        onCloseExplorerScreen = onCloseExplorerScreen,
    )

    UploadingFiles(
        onNameCollisions = { collisions ->
            onCloseExplorerScreen()
            nameCollisionLauncher.launch(collisions)
        },
        parentNodeId = parentNodeId,
        chatIds = chatIds,
        viewModel = viewModel,
        urisEvent = uploadUrisEventState.event,
        onUrisConsumed = uploadUrisEventState::consume,
        pitagTrigger = pitagTrigger,
        onStartUpload = { transferTriggerEvent ->
            onStartUpload(transferTriggerEvent)

            if (chatIds.isNullOrEmpty()) {
                onCloseExplorerScreen()
            } else {
                onSharedToChats(chatIds)
            }
        },
    )
}