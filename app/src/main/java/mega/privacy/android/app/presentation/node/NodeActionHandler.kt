package mega.privacy.android.app.presentation.node

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.activities.contract.HiddenNodeOnboardingActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToCopyActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToMoveActivityContract
import mega.privacy.android.app.activities.contract.SendToChatActivityContract
import mega.privacy.android.app.activities.contract.ShareFolderActivityContract
import mega.privacy.android.app.activities.contract.VersionsFileActivityContract
import mega.privacy.android.app.presentation.node.model.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.ClearSelectionMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.CopyMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.DownloadMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.HideDropdownMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.HideMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.MoveMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.OpenWithMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.RestoreMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.SelectAllMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.SendToChatMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.ShareFolderMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.VersionsMenuAction
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.shared.original.core.ui.model.MenuAction

/**
 * Node bottom sheet action handler
 *
 * @property activity
 * @property nodeActionsViewModel
 */
@Deprecated(
    """
    This class is a temporary solution to the issue that the screens called by the node bottom sheet 
    items have not yet been refactored. As screens are refactored, the code here needs to be 
    replaced by the individual actions defined in the NodeBottomSheetMenuItem implementations
    """
)
class NodeActionHandler(
    private val activity: Activity,
    private val nodeActionsViewModel: NodeActionsViewModel,
) {

    private val selectMoveNodeActivityLauncher =
        (activity as? AppCompatActivity)?.registerForActivityResult(
            SelectFolderToMoveActivityContract()
        ) { result ->
            result?.let {
                nodeActionsViewModel.checkNodesNameCollision(
                    it.first.toList(),
                    it.second,
                    NodeNameCollisionType.MOVE
                )
            }
        }

    private val selectCopyNodeActivityLauncher =
        (activity as? AppCompatActivity)?.registerForActivityResult(
            SelectFolderToCopyActivityContract()
        ) { result ->
            result?.let {
                nodeActionsViewModel.checkNodesNameCollision(
                    it.first.toList(),
                    it.second,
                    NodeNameCollisionType.COPY
                )
            }
        }

    private val versionsActivityLauncher =
        (activity as? AppCompatActivity)?.registerForActivityResult(
            VersionsFileActivityContract()
        ) { result ->
            result?.let {
                nodeActionsViewModel.deleteVersionHistory(it)
            }
        }

    private val shareFolderActivityLauncher =
        (activity as? AppCompatActivity)?.registerForActivityResult(
            ShareFolderActivityContract()
        ) { result ->
            result?.let { (contactIds, nodeHandles) ->
                nodeActionsViewModel.contactSelectedForShareFolder(contactIds, nodeHandles)
            }
        }

    private val restoreFromRubbishLauncher =
        (activity as? AppCompatActivity)?.registerForActivityResult(
            SelectFolderToMoveActivityContract()
        ) { result ->
            result?.let { (nodeHandles, targetHandle) ->
                nodeActionsViewModel.checkNodesNameCollision(
                    nodeHandles.toList(),
                    targetHandle,
                    NodeNameCollisionType.RESTORE
                )
            }
        }

    private val sendToChatLauncher =
        (activity as? AppCompatActivity)?.registerForActivityResult(
            SendToChatActivityContract()
        ) { result ->
            result?.let { (nodeHandles, chatIds, userHandles) ->
                nodeActionsViewModel.attachNodeToChats(
                    nodeHandles = nodeHandles,
                    chatIds = chatIds,
                    userHandles = userHandles
                )
            }
        }

    private val hiddenNodesOnboardingLauncher =
        (activity as? AppCompatActivity)?.registerForActivityResult(
            HiddenNodeOnboardingActivityContract()
        ) { result ->
            nodeActionsViewModel.handleHiddenNodesOnboardingResult(result)
        }

    /**
     * handles actions from bottom sheet
     *
     * @param action
     * @param node
     */
    fun handleAction(action: MenuAction, node: TypedNode) {
        nodeActionsViewModel.updateSelectedNodes(listOf(node))
        when (action) {
            is VersionsMenuAction -> versionsActivityLauncher?.launch(node.id.longValue)
            is MoveMenuAction -> selectMoveNodeActivityLauncher?.launch(longArrayOf(node.id.longValue))
            is CopyMenuAction -> selectCopyNodeActivityLauncher?.launch(longArrayOf(node.id.longValue))
            is ShareFolderMenuAction -> shareFolderActivityLauncher?.launch(longArrayOf(node.id.longValue))
            is RestoreMenuAction -> restoreFromRubbishLauncher?.launch(longArrayOf(node.id.longValue))
            is SendToChatMenuAction -> sendToChatLauncher?.launch(longArrayOf(node.id.longValue))
            is OpenWithMenuAction -> nodeActionsViewModel.downloadNodeForPreview()
            is DownloadMenuAction -> nodeActionsViewModel.downloadNode()
            is AvailableOfflineMenuAction -> nodeActionsViewModel.downloadNodeForOffline()
            is HideMenuAction -> {
                (activity as? AppCompatActivity)?.lifecycleScope?.launch {
                    hiddenNodesOnboardingLauncher?.launch(nodeActionsViewModel.isOnboarding())
                }
            }

            else -> throw NotImplementedError("Action $action does not have a handler.")
        }
    }

    /**
     * handle actions from toolbar
     *
     * @param action
     * @param nodes
     */
    fun handleAction(action: MenuAction, nodes: List<TypedNode>) {
        nodeActionsViewModel.updateSelectedNodes(nodes)
        when (action) {
            is OpenWithMenuAction -> nodeActionsViewModel.downloadNodeForPreview()
            is DownloadMenuAction -> nodeActionsViewModel.downloadNode()
            is AvailableOfflineMenuAction -> nodeActionsViewModel.downloadNodeForOffline()
            is ShareFolderMenuAction -> {
                val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
                shareFolderActivityLauncher?.launch(nodeHandleArray)
            }

            is CopyMenuAction -> {
                val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
                selectCopyNodeActivityLauncher?.launch(nodeHandleArray)
            }

            is MoveMenuAction -> {
                val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
                selectMoveNodeActivityLauncher?.launch(nodeHandleArray)
            }

            is SendToChatMenuAction -> {
                val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
                sendToChatLauncher?.launch(nodeHandleArray)
            }

            is SelectAllMenuAction -> {
                nodeActionsViewModel.selectAllClicked()
            }

            is ClearSelectionMenuAction -> {
                nodeActionsViewModel.clearAllClicked()
            }

            is RestoreMenuAction -> {
                val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
                restoreFromRubbishLauncher?.launch(nodeHandleArray)
            }

            is HideDropdownMenuAction -> {
                (activity as? AppCompatActivity)?.lifecycleScope?.launch {
                    hiddenNodesOnboardingLauncher?.launch(nodeActionsViewModel.isOnboarding())
                }
            }

            else -> throw NotImplementedError("Action $action does not have a handler.")
        }
    }
}