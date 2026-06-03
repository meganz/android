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
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderDialogNavKey
import mega.privacy.android.core.nodecomponents.dialog.sharefolder.ShareFolderDialogResult
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.destination.CopyNavKey
import mega.privacy.android.navigation.destination.CopyResult
import mega.privacy.android.navigation.destination.MoveNavKey
import mega.privacy.android.navigation.destination.MoveResult
import mega.privacy.android.shared.resources.R as sharedResR

/**
 * Handler to process results from node options bottom sheet.
 *
 * @param nodeOptionsActionViewModel ViewModel for node actions
 * @param navigationHandler Handler for navigation
 * @param nodeActionHandler Handler for single node actions. To gate [mega.privacy.android.core.nodecomponents.menu.menuaction.DeferrableMenuAction]
 *  actions (e.g. for rewarded ads), pass `onDeferredAction` to [mega.privacy.android.core.nodecomponents.action.rememberSingleNodeActionHandler].
 * @param onTransfer Callback for transfer events
 * @param onNavResultConsumed Optional callback invoked after the navigation result is consumed
 */
@Composable
fun HandleNodeOptionsActionResult(
    nodeOptionsActionViewModel: NodeOptionsActionViewModel,
    navigationHandler: NavigationHandler,
    nodeActionHandler: SingleNodeActionHandler = rememberSingleNodeActionHandler(
        viewModel = nodeOptionsActionViewModel,
        navigationHandler = navigationHandler,
    ),
    onTransfer: (TransferTriggerEvent) -> Unit,
    onNavResultConsumed: ((NodeOptionsBottomSheetResult) -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarQueue = rememberSnackBarQueue()
    val localResources = LocalResources.current
    val nodeBottomSheetResult by navigationHandler
        .monitorResult<NodeOptionsBottomSheetResult?>(NodeOptionsBottomSheetNavKey.RESULT)
        .collectAsStateWithLifecycle(null)
    val nodeActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()

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

    // Monitor the copy destination-folder picker result (single-activity copy flow)
    val copyResult by navigationHandler
        .monitorResult<CopyResult?>(CopyNavKey.RESULT)
        .collectAsStateWithLifecycle(null)

    LaunchedEffect(copyResult) {
        copyResult?.let { result ->
            nodeOptionsActionViewModel.checkCopyNameCollision(
                sourceHandles = result.sourceHandles,
                targetHandle = result.target.longValue,
            )
            navigationHandler.clearResult(CopyNavKey.RESULT)
        }
    }

    // Monitor the move destination-folder picker result (single-activity move flow).
    val moveResult by navigationHandler
        .monitorResult<MoveResult?>(MoveNavKey.RESULT)
        .collectAsStateWithLifecycle(null)

    LaunchedEffect(moveResult) {
        moveResult?.let { result ->
            nodeOptionsActionViewModel.checkMoveNameCollision(
                sourceHandles = result.sourceHandles,
                targetHandle = result.target.longValue,
            )
            navigationHandler.clearResult(MoveNavKey.RESULT)
        }
    }

    HandleNodeOptionsActionEvent(
        nodeActionState = nodeActionState,
        onCopyNodes = nodeOptionsActionViewModel::copyNodes,
        onMoveNodes = nodeOptionsActionViewModel::moveNodes,
        onRestoreNodes = nodeOptionsActionViewModel::restoreNodes,
        onCopyPublicLinkFiles = nodeOptionsActionViewModel::copyPublicLinkFile,
        onTransfer = onTransfer,
        onNavigate = navigationHandler::navigate,
        onShareContactSelected = nodeOptionsActionViewModel::contactSelectedForShareFolder,
        consumeNameCollisionResult = nodeOptionsActionViewModel::markHandleNodeNameCollisionResult,
        consumePublicCopyCollisionResult = nodeOptionsActionViewModel::markHandlePublicCopyCollisionResult,
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
        nodeActionHandler(result.action, result.node)
        onNavResultConsumed?.invoke(result)
        navigationHandler.clearResult(NodeOptionsBottomSheetNavKey.RESULT)
    }
}
