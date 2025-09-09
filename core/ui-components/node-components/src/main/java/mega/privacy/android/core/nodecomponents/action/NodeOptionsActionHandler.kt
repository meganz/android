package mega.privacy.android.core.nodecomponents.action

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.navigation.megaActivityResultContract

/**
 * Composable function that provides single node action handling functionality.
 *
 * This Composable automatically handles activity result contracts and provides
 * a function to handle various menu actions for a single node. It uses the AndroidX
 * Activity Result API internally and manages its own lifecycle.
 *
 * @param nodeOptionsActionViewModel The view model for handling node actions and callbacks
 * @param coroutineScope Optional coroutine scope, defaults to rememberCoroutineScope()
 * @return A function that handles menu actions for a single node
 */
@Composable
internal fun rememberSingleNodeActionHandler(
    nodeOptionsActionViewModel: NodeOptionsActionViewModel,
    coroutineScope: CoroutineScope,
): (MenuAction, TypedNode) -> Unit {
    val context = LocalContext.current
    val megaActivityResultContract = remember { context.megaActivityResultContract }

    val versionsLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.versionsFileActivityResultContract
    ) { result ->
        result?.let { nodeOptionsActionViewModel.deleteVersionHistory(it) }
    }

    val moveLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.selectFolderToMoveActivityResultContract
    ) { result ->
        result?.let { (nodeHandles, targetHandle) ->
            nodeOptionsActionViewModel.checkNodesNameCollision(
                nodeHandles.toList(),
                targetHandle,
                NodeNameCollisionType.MOVE
            )
        }
    }

    val copyLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.selectFolderToCopyActivityResultContract
    ) { result ->
        result?.let { (nodeHandles, targetHandle) ->
            nodeOptionsActionViewModel.checkNodesNameCollision(
                nodeHandles.toList(),
                targetHandle,
                NodeNameCollisionType.COPY
            )
        }
    }

    val shareFolderLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.shareFolderActivityResultContract
    ) { result ->
        result?.let { (contactIds, nodeHandles) ->
            nodeOptionsActionViewModel.contactSelectedForShareFolder(
                contactIds,
                nodeHandles
            )
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.selectFolderToMoveActivityResultContract
    ) { result ->
        result?.let { (nodeHandles, targetHandle) ->
            nodeOptionsActionViewModel.checkNodesNameCollision(
                nodeHandles.toList(),
                targetHandle,
                NodeNameCollisionType.RESTORE
            )
        }
    }

    val sendToChatLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.sendToChatActivityResultContract
    ) { result ->
        result?.let { sendToChatResult ->
            nodeOptionsActionViewModel.attachNodeToChats(
                nodeHandles = sendToChatResult.nodeIds,
                chatIds = sendToChatResult.chatIds,
                userHandles = sendToChatResult.userHandles
            )
        }
    }

    val hiddenNodesOnboardingLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.hiddenNodeOnboardingActivityResultContract
    ) { result ->
        nodeOptionsActionViewModel.handleHiddenNodesOnboardingResult(result)
    }

    return remember(
        nodeOptionsActionViewModel,
        versionsLauncher,
        moveLauncher,
        copyLauncher,
        shareFolderLauncher,
        restoreLauncher,
        sendToChatLauncher,
        hiddenNodesOnboardingLauncher,
        coroutineScope,
    ) {
        { action, node ->
            nodeOptionsActionViewModel.updateSelectedNodes(listOf(node))

            val actionContext = SingleNodeActionProvider(
                viewModel = nodeOptionsActionViewModel,
                coroutineScope = coroutineScope,
                postMessage = nodeOptionsActionViewModel::postMessage,
                versionsLauncher = versionsLauncher,
                moveLauncher = moveLauncher,
                copyLauncher = copyLauncher,
                shareFolderLauncher = shareFolderLauncher,
                restoreLauncher = restoreLauncher,
                sendToChatLauncher = sendToChatLauncher,
                hiddenNodesOnboardingLauncher = hiddenNodesOnboardingLauncher
            )

            nodeOptionsActionViewModel.handleSingleNodeAction(action) { handler ->
                handler.handle(action, node, actionContext)
            }
        }
    }
}

/**
 * Composable function that provides multiple nodes action handling functionality.
 *
 * @param nodeOptionsActionViewModel The view model for handling node actions and callbacks
 * @param coroutineScope Optional coroutine scope, defaults to rememberCoroutineScope()
 * @return A function that handles menu actions for multiple nodes
 */
@Composable
internal fun rememberMultipleNodesActionHandler(
    nodeOptionsActionViewModel: NodeOptionsActionViewModel,
    coroutineScope: CoroutineScope,
): (MenuAction, List<TypedNode>) -> Unit {
    val context = LocalContext.current
    val megaActivityResultContract = remember { context.megaActivityResultContract }

    val moveLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.selectFolderToMoveActivityResultContract
    ) { result ->
        result?.let { (nodeHandles, targetHandle) ->
            nodeOptionsActionViewModel.checkNodesNameCollision(
                nodeHandles.toList(),
                targetHandle,
                NodeNameCollisionType.MOVE
            )
        }
    }

    val copyLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.selectFolderToCopyActivityResultContract
    ) { result ->
        result?.let { (nodeHandles, targetHandle) ->
            nodeOptionsActionViewModel.checkNodesNameCollision(
                nodeHandles.toList(),
                targetHandle,
                NodeNameCollisionType.COPY
            )
        }
    }

    val shareFolderLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.shareFolderActivityResultContract
    ) { result ->
        result?.let { (contactIds, nodeHandles) ->
            nodeOptionsActionViewModel.contactSelectedForShareFolder(
                contactIds,
                nodeHandles
            )
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.selectFolderToMoveActivityResultContract
    ) { result ->
        result?.let { (nodeHandles, targetHandle) ->
            nodeOptionsActionViewModel.checkNodesNameCollision(
                nodeHandles.toList(),
                targetHandle,
                NodeNameCollisionType.RESTORE
            )
        }
    }

    val sendToChatLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.sendToChatActivityResultContract
    ) { result ->
        result?.let { sendToChatResult ->
            nodeOptionsActionViewModel.attachNodeToChats(
                nodeHandles = sendToChatResult.nodeIds,
                chatIds = sendToChatResult.chatIds,
                userHandles = sendToChatResult.userHandles
            )
        }
    }

    val hiddenNodesOnboardingLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.hiddenNodeOnboardingActivityResultContract
    ) { result ->
        nodeOptionsActionViewModel.handleHiddenNodesOnboardingResult(result)
    }

    return remember(
        nodeOptionsActionViewModel,
        moveLauncher,
        copyLauncher,
        shareFolderLauncher,
        restoreLauncher,
        sendToChatLauncher,
        hiddenNodesOnboardingLauncher,
        coroutineScope
    ) {
        { action, nodes ->
            nodeOptionsActionViewModel.updateSelectedNodes(nodes)

            val actionContext = MultipleNodesActionProvider(
                viewModel = nodeOptionsActionViewModel,
                coroutineScope = coroutineScope,
                postMessage = nodeOptionsActionViewModel::postMessage,
                moveLauncher = moveLauncher,
                copyLauncher = copyLauncher,
                shareFolderLauncher = shareFolderLauncher,
                restoreLauncher = restoreLauncher,
                sendToChatLauncher = sendToChatLauncher,
                hiddenNodesOnboardingLauncher = hiddenNodesOnboardingLauncher
            )

            nodeOptionsActionViewModel.handleMultipleNodesAction(action) { handler ->
                handler.handle(action, nodes, actionContext)
            }
        }
    }
}

/**
 * Creates a NodeActionHandler from Composable functions.
 *
 * @param viewModel The view model for handling node actions
 * @param coroutineScope Optional coroutine scope. Defaults to rememberCoroutineScope()
 * @return A NodeActionHandler instance
 * @see NodeActionHandler
 */
@Composable
fun rememberNodeActionHandler(
    viewModel: NodeOptionsActionViewModel = hiltViewModel(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
): NodeActionHandler {
    val singleNodeHandler =
        rememberSingleNodeActionHandler(viewModel, coroutineScope)
    val multipleNodesHandler =
        rememberMultipleNodesActionHandler(viewModel, coroutineScope)

    return remember(singleNodeHandler, multipleNodesHandler) {
        NodeActionHandler(singleNodeHandler, multipleNodesHandler)
    }
}

