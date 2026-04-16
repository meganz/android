package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalResources
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import mega.android.core.ui.model.SnackbarAttributes
import mega.android.core.ui.model.SnackbarDuration
import mega.privacy.android.core.nodecomponents.action.HandleNodeOptionsActionEvent
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderDialogNavKey
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderDialogResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.shared.resources.R as sharedResR

/**
 * Handler to process results from node options bottom sheet
 *
 * @param nodeOptionsActionViewModel
 * @param navigationHandler
 * @param onTransfer
 * @param onActionExecuted Optional callback
 */
@Composable
fun HandleNodeOptionsActionResult(
    nodeOptionsActionViewModel: NodeOptionsActionViewModel,
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
    onActionExecuted: ((NodeOptionsBottomSheetResult) -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarQueue = rememberSnackBarQueue()
    val localResources = LocalResources.current
    val nodeBottomSheetResult by navigationHandler
        .monitorResult<NodeOptionsBottomSheetResult?>(NodeOptionsBottomSheetNavKey.RESULT)
        .collectAsStateWithLifecycle(null)
    val nodeActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()

    val actionHandler = rememberSingleNodeActionHandler(
        viewModel = nodeOptionsActionViewModel,
        navigationHandler = navigationHandler,
    )

    // Monitor share folder dialog result (moved from bottom sheet)
    val shareFolderDialogResult by navigationHandler
        .monitorResult<ShareFolderDialogResult?>(ShareFolderDialogNavKey.RESULT)
        .collectAsStateWithLifecycle(null)

    LaunchedEffect(shareFolderDialogResult) {
        shareFolderDialogResult?.let { result ->
            val handles = result.nodes.map { it.id.longValue }
            nodeOptionsActionViewModel.triggerShareFolderFromDialogResult(handles)
            navigationHandler.clearResult(ShareFolderDialogNavKey.RESULT)
        }
    }

    HandleNodeOptionsActionEvent(
        nodeActionState = nodeActionState,
        onCopyNodes = nodeOptionsActionViewModel::copyNodes,
        onMoveNodes = nodeOptionsActionViewModel::moveNodes,
        onRestoreNodes = nodeOptionsActionViewModel::restoreNodes,
        onTransfer = onTransfer,
        onNavigate = navigationHandler::navigate,
        onShareContactSelected = nodeOptionsActionViewModel::contactSelectedForShareFolder,
        consumeNameCollisionResult = nodeOptionsActionViewModel::markHandleNodeNameCollisionResult,
        consumeInfoToShow = nodeOptionsActionViewModel::onInfoToShowEventConsumed,
        consumeForeignNodeDialog = nodeOptionsActionViewModel::markForeignNodeDialogShown,
        consumeQuotaDialog = nodeOptionsActionViewModel::markQuotaDialogShown,
        consumeDownloadEvent = nodeOptionsActionViewModel::markDownloadEventConsumed,
        consumeRenameNodeRequest = nodeOptionsActionViewModel::resetRenameNodeRequest,
        consumeNavigationEvent = nodeOptionsActionViewModel::resetNavigationEvent,
        consumeDismissEvent = nodeOptionsActionViewModel::resetDismiss,
        consumeAccessDialogShown = nodeOptionsActionViewModel::markShareFolderAccessDialogShown,
        consumeShareFolderEvent = nodeOptionsActionViewModel::resetShareFolderEvent,
        consumeShareFolderDialogEvent = nodeOptionsActionViewModel::resetShareFolderDialogEvent,
        onActionTriggered = { nodeOptionsActionViewModel.onActionTriggered() },
        onRestoreSuccess = { data ->
            val locateActionLabel = localResources.getString(
                sharedResR.string.transfers_notification_location_action
            )
            coroutineScope.launch {
                snackbarQueue.queueMessage(
                    SnackbarAttributes(
                        message = data.message,
                        duration = SnackbarDuration.Long,
                        action = locateActionLabel,
                        actionClick = {
                            navigationHandler.navigate(
                                CloudDriveNavKey(
                                    nodeHandle = data.parentHandle,
                                    highlightedNodeHandle = data.restoredNodeHandle
                                )
                            )
                        }
                    )
                )
            }
        },
        consumeRestoreSuccess = nodeOptionsActionViewModel::resetRestoreSuccessEvent,
    )

    LaunchedEffect(nodeBottomSheetResult) {
        val result = nodeBottomSheetResult ?: return@LaunchedEffect
        actionHandler(result.action, result.node)
        onActionExecuted?.invoke(result)
        navigationHandler.clearResult(NodeOptionsBottomSheetNavKey.RESULT)
    }
}
