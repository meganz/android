package mega.privacy.android.core.nodecomponents.sheet.options

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mega.privacy.android.core.nodecomponents.action.NodeOptionsActionViewModel
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.queue.snackbar.rememberSnackBarQueue
import mega.privacy.android.navigation.extensions.rememberMegaResultContract
import timber.log.Timber

@Composable
fun HandleNodeOptionsResult(
    nodeOptionsActionViewModel: NodeOptionsActionViewModel,
    onNavigate: (NavKey) -> Unit,
    onTransfer: (TransferTriggerEvent) -> Unit,
    nodeResultFlow: (String) -> Flow<NodeOptionsBottomSheetResult?>,
    clearResultFlow: (String) -> Unit,
) {
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
    val sendToChatLauncher = rememberLauncherForActivityResult(
        contract = megaResultContract.sendToChatActivityResultContract
    ) { result ->
        result?.let { sendToChatResult ->
            nodeOptionsActionViewModel.attachNodeToChats(
                nodeHandles = sendToChatResult.nodeIds,
                chatIds = sendToChatResult.chatIds,
                userHandles = sendToChatResult.userHandles
            )
        }
    }

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

            else -> return@LaunchedEffect
        }

        clearResultFlow(NodeOptionsBottomSheetNavKey.RESULT)
    }
}