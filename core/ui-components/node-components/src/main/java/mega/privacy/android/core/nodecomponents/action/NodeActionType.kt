package mega.privacy.android.core.nodecomponents.action

import kotlinx.coroutines.launch
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ClearSelectionMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.OpenWithMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RenameMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RestoreMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SelectAllMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.SendToChatMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ShareFolderMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.VersionsMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
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
        provider.shareFolderLauncher.launch(longArrayOf(node.id.longValue))
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
        provider.shareFolderLauncher.launch(nodeHandleArray)
    }
}

class RestoreAction @Inject constructor() : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is RestoreMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.restoreLauncher.launch(longArrayOf(node.id.longValue))
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
        provider.restoreLauncher.launch(nodeHandleArray)
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