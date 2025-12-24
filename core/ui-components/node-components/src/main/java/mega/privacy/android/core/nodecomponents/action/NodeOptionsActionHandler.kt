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
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
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
    navigationHandler: NavigationHandler?,
    megaNavigator: MegaNavigator,
    coroutineScope: CoroutineScope,
): (MenuAction, TypedNode) -> Unit {
    val context = LocalContext.current
    val megaActivityResultContract = remember { context.megaActivityResultContract }

    val versionsLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.versionsFileActivityResultContract
    ) { result ->
        nodeOptionsActionViewModel.deleteVersionHistory(result)
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
        contract = megaActivityResultContract.nameCollisionActivityContract
    ) { message ->
        if (!message.isNullOrEmpty()) {
            nodeOptionsActionViewModel.postMessage(message)
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
        nodeOptionsActionViewModel.handleHiddenNodesOnboardingResult(result, true)
    }

    val addToAlbumLauncher =
        rememberLauncherForActivityResult(
            contract = megaActivityResultContract.addToAlbumResultContract
        ) { message ->
            if (!message.isNullOrEmpty()) {
                nodeOptionsActionViewModel.postMessage(message)
            }
        }

    val videoToPlaylistLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.videoToPlaylistActivityContract
    ) { result ->
        result?.let {
            nodeOptionsActionViewModel.triggerAddVideoToPlaylistResultEvent(it)
        }
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
        addToAlbumLauncher,
        videoToPlaylistLauncher,
        coroutineScope,
        navigationHandler,
        megaNavigator
    ) {
        { action, node ->
            nodeOptionsActionViewModel.updateSelectedNodes(listOf(node))

            val actionContext = SingleNodeActionProvider(
                viewModel = nodeOptionsActionViewModel,
                context = context,
                coroutineScope = coroutineScope,
                postMessage = nodeOptionsActionViewModel::postMessage,
                navigationHandler = navigationHandler,
                megaNavigator = megaNavigator,
                versionsLauncher = versionsLauncher,
                moveLauncher = moveLauncher,
                copyLauncher = copyLauncher,
                shareFolderLauncher = shareFolderLauncher,
                restoreLauncher = restoreLauncher,
                sendToChatLauncher = sendToChatLauncher,
                hiddenNodesOnboardingLauncher = hiddenNodesOnboardingLauncher,
                addToAlbumLauncher = addToAlbumLauncher,
                videoToPlaylistLauncher = videoToPlaylistLauncher
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
    navigationHandler: NavigationHandler?,
    megaNavigator: MegaNavigator,
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
        contract = megaActivityResultContract.nameCollisionActivityContract
    ) { message ->
        if (!message.isNullOrEmpty()) {
            nodeOptionsActionViewModel.postMessage(message)
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
        nodeOptionsActionViewModel.handleHiddenNodesOnboardingResult(result, true)
    }

    val addToAlbumLauncher =
        rememberLauncherForActivityResult(
            contract = megaActivityResultContract.addToAlbumResultContract
        ) { message ->
            if (!message.isNullOrEmpty()) {
                nodeOptionsActionViewModel.postMessage(message)
            }
        }

    return remember(
        nodeOptionsActionViewModel,
        moveLauncher,
        copyLauncher,
        shareFolderLauncher,
        restoreLauncher,
        sendToChatLauncher,
        hiddenNodesOnboardingLauncher,
        addToAlbumLauncher,
        coroutineScope,
        navigationHandler,
        megaNavigator
    ) {
        { action, nodes ->
            nodeOptionsActionViewModel.updateSelectedNodes(nodes)

            val actionContext = MultipleNodesActionProvider(
                viewModel = nodeOptionsActionViewModel,
                context = context,
                coroutineScope = coroutineScope,
                postMessage = nodeOptionsActionViewModel::postMessage,
                navigationHandler = navigationHandler,
                megaNavigator = megaNavigator,
                moveLauncher = moveLauncher,
                copyLauncher = copyLauncher,
                shareFolderLauncher = shareFolderLauncher,
                restoreLauncher = restoreLauncher,
                sendToChatLauncher = sendToChatLauncher,
                hiddenNodesOnboardingLauncher = hiddenNodesOnboardingLauncher,
                addToAlbumLauncher = addToAlbumLauncher
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
    megaNavigator: MegaNavigator = rememberMegaNavigator(),
    navigationHandler: NavigationHandler? = null,
): NodeActionHandler {
    val singleNodeHandler =
        rememberSingleNodeActionHandler(
            nodeOptionsActionViewModel = viewModel,
            navigationHandler = navigationHandler,
            megaNavigator = megaNavigator,
            coroutineScope = coroutineScope
        )
    val multipleNodesHandler =
        rememberMultipleNodesActionHandler(
            nodeOptionsActionViewModel = viewModel,
            navigationHandler = navigationHandler,
            megaNavigator = megaNavigator,
            coroutineScope = coroutineScope
        )

    return remember(singleNodeHandler, multipleNodesHandler) {
        NodeActionHandler(
            singleNodeHandler = singleNodeHandler,
            multipleNodesHandler = multipleNodesHandler
        )
    }
}

