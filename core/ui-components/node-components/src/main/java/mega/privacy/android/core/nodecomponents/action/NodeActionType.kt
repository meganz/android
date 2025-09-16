package mega.privacy.android.core.nodecomponents.action

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.dialog.delete.MoveToRubbishOrDeleteDialogArgs
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.mapper.RestoreNodeResultMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ClearSelectionMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DeletePermanentlyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.OpenWithMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RenameMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RestoreMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SelectAllMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SendToChatMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareFolderMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.VersionsMenuAction
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.RestoreNodesUseCase
import timber.log.Timber
import javax.inject.Inject

interface BaseNodeAction {
    fun canHandle(action: MenuAction): Boolean
}

/**
 * Handler for single node operations.
 */
interface SingleNodeAction : BaseNodeAction {
    fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider)
}

/**
 * Handler for multiple nodes operations.
 */
interface MultiNodeAction : BaseNodeAction {
    fun handle(action: MenuAction, nodes: List<TypedNode>, provider: MultipleNodesActionProvider)
}

class VersionsAction @Inject constructor() : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is VersionsMenuAction
    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.versionsLauncher.launch(node.id.longValue)
    }
}

class MoveAction @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is MoveMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.moveLauncher.launch(longArrayOf(node.id.longValue))
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
        provider.moveLauncher.launch(nodeHandleArray)
    }
}

class CopyAction @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is CopyMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.copyLauncher.launch(longArrayOf(node.id.longValue))
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
        provider.copyLauncher.launch(nodeHandleArray)
    }
}

class ShareFolderAction @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is ShareFolderMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.viewModel.verifyShareFolderAction(node)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        provider.viewModel.verifyShareFolderAction(nodes)
    }
}

class RestoreAction @Inject constructor(
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase,
    private val restoreNodesUseCase: RestoreNodesUseCase,
    private val restoreNodeResultMapper: RestoreNodeResultMapper,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is RestoreMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        handleRestore(listOf(node), provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        handleRestore(nodes, provider)
    }

    private fun handleRestore(
        nodes: List<TypedNode>,
        provider: NodeActionProvider,
    ) {
        provider.coroutineScope.launch {
            withContext(NonCancellable) {
                val restoreMap = nodes.associate { node ->
                    node.id.longValue to (node.restoreId?.longValue ?: -1L)
                }
                runCatching {
                    checkNodesNameCollisionUseCase(restoreMap, NodeNameCollisionType.RESTORE)
                }.onSuccess { result ->
                    if (result.conflictNodes.isNotEmpty()) {
                        provider.coroutineScope.ensureActive()
                        val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
                        provider.restoreLauncher.launch(nodeHandleArray)
                    }
                    if (result.noConflictNodes.isNotEmpty()) {
                        val restoreResult = restoreNodesUseCase(result.noConflictNodes)
                        val message = restoreNodeResultMapper(restoreResult)
                        provider.postMessage(message)
                    }
                }.onFailure { throwable ->
                    Timber.e(throwable)
                }
            }
        }
    }
}

class SendToChatAction @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is SendToChatMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.sendToChatLauncher.launch(longArrayOf(node.id.longValue))
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
        provider.sendToChatLauncher.launch(nodeHandleArray)
    }
}

class OpenWithAction @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is OpenWithMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.viewModel.downloadNodeForPreview(true)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        provider.viewModel.downloadNodeForPreview(true)
    }
}

class DownloadAction @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is DownloadMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.viewModel.downloadNode(withStartMessage = false)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        provider.viewModel.downloadNode(withStartMessage = false)
    }
}

class AvailableOfflineAction @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is AvailableOfflineMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.viewModel.downloadNodeForOffline(withStartMessage = false)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        provider.viewModel.downloadNodeForOffline(withStartMessage = false)
    }
}

class HideAction @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is HideMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.coroutineScope.launch {
            provider.hiddenNodesOnboardingLauncher.launch(provider.viewModel.isOnboarding())
        }
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        provider.coroutineScope.launch {
            // Todo add analytics when available
            //Analytics.tracker.trackEvent(HideNodeMultiSelectMenuItemEvent)
            provider.hiddenNodesOnboardingLauncher.launch(provider.viewModel.isOnboarding())
        }
    }
}

class SelectAllAction @Inject constructor() : MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is SelectAllMenuAction
    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        provider.viewModel.selectAllClicked()
    }
}

class ClearSelectionAction @Inject constructor() : MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is ClearSelectionMenuAction
    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        provider.viewModel.clearAllClicked()
    }
}

class RenameNodeAction @Inject constructor() : SingleNodeAction {
    override fun canHandle(action: MenuAction): Boolean =
        action is RenameMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.viewModel.handleRenameNodeRequest(node.id)
    }
}

class MoveToRubbishBinAction @Inject constructor(
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is TrashMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        handleTrashAction(listOf(node), provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        handleTrashAction(nodes, provider)
    }

    private fun handleTrashAction(
        nodes: List<TypedNode>,
        provider: NodeActionProvider,
    ) {
        provider.coroutineScope.launch {
            val handles = nodes.map { it.id.longValue }
            runCatching { nodeHandlesToJsonMapper(handles) }
                .onSuccess {
                    provider.navigationHandler?.navigate(
                        MoveToRubbishOrDeleteDialogArgs(
                            isInRubbish = false,
                            nodeHandles = handles
                        )
                    )
                }
                .onFailure { Timber.e(it) }
        }
    }
}

class ManageLinkAction @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is ManageLinkMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.megaNavigator.openGetLinkActivity(
            context = provider.context,
            handle = node.id.longValue
        )
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        val handles = nodes.map { it.id.longValue }.toLongArray()
        provider.megaNavigator.openGetLinkActivity(
            context = provider.context,
            handles = handles
        )
    }
}

class DeletePermanentAction @Inject constructor(
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is DeletePermanentlyMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        deleteNodes(listOf(node), provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        deleteNodes(nodes, provider)
    }

    private fun deleteNodes(
        nodes: List<TypedNode>,
        provider: NodeActionProvider,
    ) {
        provider.coroutineScope.launch {
            withContext(NonCancellable) {
                val handles = nodes.map { it.id.longValue }
                runCatching { nodeHandlesToJsonMapper(handles) }
                    .onSuccess {
                        provider.navigationHandler?.navigate(
                            MoveToRubbishOrDeleteDialogArgs(
                                isInRubbish = true,
                                nodeHandles = handles
                            )
                        )
                    }
                    .onFailure { Timber.e(it) }
            }
        }
    }
}