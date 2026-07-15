package mega.privacy.android.app.presentation.node

import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.app.activities.contract.HiddenNodeOnboardingActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToCopyActivityContract
import mega.privacy.android.app.activities.contract.SelectFolderToMoveActivityContract
import mega.privacy.android.app.activities.contract.SendToChatActivityContract
import mega.privacy.android.app.activities.contract.VersionsFileActivityContract
import mega.privacy.android.app.activities.contract.VideoToPlaylistActivityContract
import mega.privacy.android.app.main.legacycontact.AddContactActivity
import mega.privacy.android.app.presentation.node.model.menuaction.AddToPlaylistMenuAction
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
import mega.privacy.android.app.presentation.node.model.menuaction.UnhideDropdownMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.UnhideMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.VersionsMenuAction
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.navigation.megaNavigator

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
            ActivityResultContracts.StartActivityForResult()
        ) { activityResult ->
            val intent = activityResult.data
            val result = if (activityResult.resultCode == Activity.RESULT_OK && intent?.extras != null) {
                val contactsData = intent.getStringArrayListExtra(AddContactActivity.EXTRA_CONTACTS)
                when (intent.getIntExtra("MULTISELECT", -1)) {
                    0 -> {
                        val nodeHandle = intent.getLongExtra(AddContactActivity.EXTRA_NODE_HANDLE, -1)
                        (contactsData ?: emptyList<String>()) to listOf(nodeHandle)
                    }

                    1 -> {
                        val nodeHandles =
                            intent.getLongArrayExtra(AddContactActivity.EXTRA_NODE_HANDLE)?.toList()
                        (contactsData ?: emptyList<String>()) to (nodeHandles ?: emptyList())
                    }

                    else -> null
                }
            } else {
                null
            }
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
            nodeActionsViewModel.handleHiddenNodesOnboardingResult(result, true)
        }

    private val addToPlaylistLauncher =
        (activity as? AppCompatActivity)?.registerForActivityResult(
            VideoToPlaylistActivityContract()
        ) { result ->
            result?.let {
                nodeActionsViewModel.triggerAddVideoToPlaylistResultEvent(it)
            }
        }

    private fun launchCopy(handles: LongArray) {
        selectCopyNodeActivityLauncher?.launch(handles)
    }

    private fun launchMove(handles: LongArray) {
        selectMoveNodeActivityLauncher?.launch(handles)
    }

    private fun launchShareFolder(nodeHandles: List<Long>) {
        shareFolderActivityLauncher?.let { launcher ->
            activity.megaNavigator.openAddContactToShare(
                context = activity,
                launcher = launcher,
                nodeHandles = nodeHandles,
            )
        }
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
            is MoveMenuAction -> launchMove(longArrayOf(node.id.longValue))
            is CopyMenuAction -> launchCopy(longArrayOf(node.id.longValue))
            is ShareFolderMenuAction -> launchShareFolder(listOf(node.id.longValue))
            is RestoreMenuAction -> restoreFromRubbishLauncher?.launch(longArrayOf(node.id.longValue))
            is SendToChatMenuAction -> sendToChatLauncher?.launch(longArrayOf(node.id.longValue))
            is OpenWithMenuAction -> nodeActionsViewModel.downloadNodeForPreview(true)
            is DownloadMenuAction -> nodeActionsViewModel.downloadNode(withStartMessage = false)
            is AvailableOfflineMenuAction -> nodeActionsViewModel.downloadNodeForOffline(
                withStartMessage = false
            )

            is HideMenuAction -> (activity as? AppCompatActivity)?.lifecycleScope?.launch {
                val isHiddenNodesOnboarded = nodeActionsViewModel.isHiddenNodesOnboarded()
                val isOnboarding = nodeActionsViewModel.isOnboarding()
                if (isOnboarding && isHiddenNodesOnboarded) {
                    nodeActionsViewModel.handleHiddenNodesOnboardingResult(
                        isOnboarded = true,
                        isHidden = true
                    )
                } else {
                    hiddenNodesOnboardingLauncher?.launch(isOnboarding)
                }
            }

            is UnhideMenuAction -> nodeActionsViewModel.handleHiddenNodesOnboardingResult(
                isOnboarded = true,
                isHidden = false
            )

            is AddToPlaylistMenuAction -> addToPlaylistLauncher?.launch(node.id.longValue)

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
            is OpenWithMenuAction -> nodeActionsViewModel.downloadNodeForPreview(true)
            is DownloadMenuAction -> nodeActionsViewModel.downloadNode(withStartMessage = false)
            is AvailableOfflineMenuAction -> nodeActionsViewModel.downloadNodeForOffline(
                withStartMessage = false,
            )

            is ShareFolderMenuAction -> {
                launchShareFolder(nodes.map { it.id.longValue })
            }

            is CopyMenuAction -> {
                val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
                launchCopy(nodeHandleArray)
            }

            is MoveMenuAction -> {
                val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
                launchMove(nodeHandleArray)
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

            is HideDropdownMenuAction -> (activity as? AppCompatActivity)?.lifecycleScope?.launch {
                val isHiddenNodesOnboarded = nodeActionsViewModel.isHiddenNodesOnboarded()
                val isOnboarding = nodeActionsViewModel.isOnboarding()
                if (isOnboarding && isHiddenNodesOnboarded) {
                    nodeActionsViewModel.handleHiddenNodesOnboardingResult(
                        isOnboarded = true,
                        isHidden = true
                    )
                } else {
                    hiddenNodesOnboardingLauncher?.launch(isOnboarding)
                }
            }

            is UnhideDropdownMenuAction -> nodeActionsViewModel.handleHiddenNodesOnboardingResult(
                isOnboarded = true,
                isHidden = false
            )

            else -> throw NotImplementedError("Action $action does not have a handler.")
        }
    }
}