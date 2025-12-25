package mega.privacy.android.core.nodecomponents.action

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.extensions.rememberMegaNavigator
import mega.privacy.android.navigation.megaActivityResultContract

/**
 * Creates a BottomSheetActionHandler for single node operations from bottom sheets.
 *
 * This Composable automatically handles activity result contracts and provides
 * a handler for various menu actions on a single node. It uses the AndroidX
 * Activity Result API internally and manages its own lifecycle.
 *
 * @param viewModel The view model for handling node actions
 * @param coroutineScope Optional coroutine scope. Defaults to rememberCoroutineScope()
 * @param megaNavigator The mega navigator instance
 * @param navigationHandler Optional navigation handler
 * @return A BottomSheetActionHandler instance
 * @see SingleNodeActionHandler
 */
@Composable
fun rememberSingleNodeActionHandler(
    viewModel: NodeOptionsActionViewModel = hiltViewModel(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    megaNavigator: MegaNavigator = rememberMegaNavigator(),
    navigationHandler: NavigationHandler? = null,
): SingleNodeActionHandler {
    val context = LocalContext.current
    val megaActivityResultContract = remember { context.megaActivityResultContract }

    val versionsLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.versionsFileActivityResultContract
    ) { result ->
        viewModel.deleteVersionHistory(result)
    }

    val moveLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.selectFolderToMoveActivityResultContract
    ) { result ->
        result?.let { (nodeHandles, targetHandle) ->
            viewModel.checkNodesNameCollision(
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
            viewModel.checkNodesNameCollision(
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
            viewModel.contactSelectedForShareFolder(
                contactIds,
                nodeHandles
            )
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.nameCollisionActivityContract
    ) { message ->
        if (!message.isNullOrEmpty()) {
            viewModel.postMessage(message)
        }
    }

    val sendToChatLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.sendToChatActivityResultContract
    ) { result ->
        result?.let { sendToChatResult ->
            viewModel.attachNodeToChats(
                nodeHandles = sendToChatResult.nodeIds,
                chatIds = sendToChatResult.chatIds,
                userHandles = sendToChatResult.userHandles
            )
        }
    }

    val hiddenNodesOnboardingLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.hiddenNodeOnboardingActivityResultContract
    ) { result ->
        viewModel.handleHiddenNodesOnboardingResult(result, true)
    }

    val addToAlbumLauncher =
        rememberLauncherForActivityResult(
            contract = megaActivityResultContract.addToAlbumResultContract
        ) { message ->
            if (!message.isNullOrEmpty()) {
                viewModel.postMessage(message)
            }
        }

    val videoToPlaylistLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.videoToPlaylistActivityContract
    ) { result ->
        result?.let {
            viewModel.triggerAddVideoToPlaylistResultEvent(it)
        }
    }

    return remember(
        viewModel,
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
        SingleNodeActionHandler { action, node ->
            viewModel.updateSelectedNodes(listOf(node))

            val actionContext = SingleNodeActionProvider(
                viewModel = viewModel,
                context = context,
                coroutineScope = coroutineScope,
                postMessage = viewModel::postMessage,
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

            viewModel.handleSingleNodeAction(action) { handler ->
                handler.handle(action, node, actionContext)
            }
        }
    }
}

/**
 * Creates a handler for multiple node operations from selection mode.
 *
 * This Composable automatically handles activity result contracts and provides
 * a handler for various menu actions on multiple nodes. It uses the AndroidX
 * Activity Result API internally and manages its own lifecycle.
 *
 * @param viewModel The view model for handling node actions
 * @param coroutineScope Optional coroutine scope. Defaults to rememberCoroutineScope()
 * @param megaNavigator The mega navigator instance
 * @param navigationHandler Optional navigation handler
 * @return A SelectionModeActionHandler instance
 * @see MultiNodeActionHandler
 */
@Composable
fun rememberMultiNodeActionHandler(
    viewModel: NodeOptionsActionViewModel = hiltViewModel(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    megaNavigator: MegaNavigator = rememberMegaNavigator(),
    navigationHandler: NavigationHandler? = null,
): MultiNodeActionHandler {
    val context = LocalContext.current
    val megaActivityResultContract = remember { context.megaActivityResultContract }

    val moveLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.selectFolderToMoveActivityResultContract
    ) { result ->
        result?.let { (nodeHandles, targetHandle) ->
            viewModel.checkNodesNameCollision(
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
            viewModel.checkNodesNameCollision(
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
            viewModel.contactSelectedForShareFolder(
                contactIds,
                nodeHandles
            )
        }
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.nameCollisionActivityContract
    ) { message ->
        if (!message.isNullOrEmpty()) {
            viewModel.postMessage(message)
        }
    }

    val sendToChatLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.sendToChatActivityResultContract
    ) { result ->
        result?.let { sendToChatResult ->
            viewModel.attachNodeToChats(
                nodeHandles = sendToChatResult.nodeIds,
                chatIds = sendToChatResult.chatIds,
                userHandles = sendToChatResult.userHandles
            )
        }
    }

    val hiddenNodesOnboardingLauncher = rememberLauncherForActivityResult(
        contract = megaActivityResultContract.hiddenNodeOnboardingActivityResultContract
    ) { result ->
        viewModel.handleHiddenNodesOnboardingResult(result, true)
    }

    val addToAlbumLauncher =
        rememberLauncherForActivityResult(
            contract = megaActivityResultContract.addToAlbumResultContract
        ) { message ->
            if (!message.isNullOrEmpty()) {
                viewModel.postMessage(message)
            }
        }

    return remember(
        viewModel,
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
        MultiNodeActionHandler { action, nodes ->
            viewModel.updateSelectedNodes(nodes)

            val actionContext = MultipleNodesActionProvider(
                viewModel = viewModel,
                context = context,
                coroutineScope = coroutineScope,
                postMessage = viewModel::postMessage,
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

            viewModel.handleMultipleNodesAction(action) { handler ->
                handler.handle(action, nodes, actionContext)
            }
        }
    }
}
