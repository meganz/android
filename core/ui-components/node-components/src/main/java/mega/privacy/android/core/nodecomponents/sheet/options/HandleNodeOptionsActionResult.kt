package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mega.android.core.ui.model.SnackbarAttributes
import mega.android.core.ui.model.SnackbarDuration
import mega.privacy.android.core.nodecomponents.action.HandleNodeOptionsActionEvent
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.core.nodecomponents.dialog.rename.RenameNodeDialogNavKey
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.destination.CloudDriveNavKey
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import mega.privacy.android.shared.resources.R as sharedResR
import timber.log.Timber

@Composable
fun HandleNodeOptionsActionResult(
    nodeOptionsActionViewModel: NodeOptionsActionViewModel,
    onNavigate: (NavKey) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    nodeResultFlow: (String) -> Flow<NodeOptionsBottomSheetResult?>,
    clearResultFlow: (String) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val megaResultContract = rememberMegaResultContract()
    val snackbarQueue = rememberSnackBarQueue()
    val nodeBottomSheetResult =
        nodeResultFlow(NodeOptionsBottomSheetNavKey.RESULT).collectAsStateWithLifecycle(null)
    val nameCollisionLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.nameCollisionActivityContract
    ) { message ->
        if (!message.isNullOrEmpty()) {
            coroutineScope.launch {
                snackbarQueue.queueMessage(message)
            }
        }
    }
    val nodeActionState by nodeOptionsActionViewModel.uiState.collectAsStateWithLifecycle()

    HandleNodeOptionsActionEvent(
        nodeActionState = nodeActionState,
        onCopyNodes = nodeOptionsActionViewModel::copyNodes,
        onMoveNodes = nodeOptionsActionViewModel::moveNodes,
        onTransfer = onTransfer,
        consumeNameCollisionResult = nodeOptionsActionViewModel::markHandleNodeNameCollisionResult,
        consumeInfoToShow = nodeOptionsActionViewModel::onInfoToShowEventConsumed,
        consumeForeignNodeDialog = nodeOptionsActionViewModel::markForeignNodeDialogShown,
        consumeQuotaDialog = nodeOptionsActionViewModel::markQuotaDialogShown,
        consumeDownloadEvent = nodeOptionsActionViewModel::markDownloadEventConsumed,
        onActionTriggered = { nodeOptionsActionViewModel.onActionTriggered() },
    )

    LaunchedEffect(nodeBottomSheetResult.value) {
        when (val result = nodeBottomSheetResult.value) {
            is NodeOptionsBottomSheetResult.Navigation -> {
                onNavigate(result.navKey)
            }

            is NodeOptionsBottomSheetResult.Transfer -> {
                onTransfer(result.event)
            }

            is NodeOptionsBottomSheetResult.Rename -> {
                nodeOptionsActionViewModel.handleRenameNodeRequest(result.nodeId)
                onNavigate(RenameNodeDialogNavKey(result.nodeId.longValue))
            }

            is NodeOptionsBottomSheetResult.NodeNameCollision -> {
                val result = result.result
                if (result.conflictNodes.isNotEmpty()) {
                    nameCollisionLauncher
                        .launch(result.conflictNodes.values.toCollection(ArrayList()))
                }
                if (result.noConflictNodes.isNotEmpty()) {
                    val nodes = result.noConflictNodes
                    when (result.type) {
                        NodeNameCollisionType.MOVE -> nodeOptionsActionViewModel.moveNodes(nodes)
                        NodeNameCollisionType.COPY -> nodeOptionsActionViewModel.copyNodes(nodes)
                        else -> Timber.d("Not implemented")
                    }
                }
            }

            is NodeOptionsBottomSheetResult.RestoreSuccess -> {
                val data = result.data
                val locateActionLabel = context.getString(
                    sharedResR.string.transfers_notification_location_action
                )
                coroutineScope.launch {
                    snackbarQueue.queueMessage(
                        SnackbarAttributes(
                            message = data.message,
                            duration = SnackbarDuration.Long,
                            action = locateActionLabel,
                            actionClick = {
                                onNavigate(
                                    CloudDriveNavKey(
                                        nodeHandle = data.parentHandle,
                                        highlightedNodeHandle = data.restoredNodeHandle
                                    )
                                )
                            }
                        )
                    )
                }
            }

            else -> return@LaunchedEffect
        }

        clearResultFlow(NodeOptionsBottomSheetNavKey.RESULT)
    }
}