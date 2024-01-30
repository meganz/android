package mega.privacy.android.app.presentation.node

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import mega.privacy.android.app.activities.contract.SelectFolderToMoveActivityContract
import mega.privacy.android.app.activities.contract.ShareFolderActivityContract
import mega.privacy.android.app.activities.contract.VersionsFileActivityContract
import mega.privacy.android.app.presentation.node.model.menuaction.CopyMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.MoveMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.RestoreMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.ShareFolderMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.VersionsMenuAction
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Node bottom sheet action handler
 *
 * @property activity
 * @property nodeOptionsBottomSheetViewModel
 */
@Deprecated(
    """
    This class is a temporary solution to the issue that the screens called by the node bottom sheet 
    items have not yet been refactored. As screens are refactored, the code here needs to be 
    replaced by the individual actions defined in the NodeBottomSheetMenuItem implementations
    """
)
class NodeBottomSheetActionHandler(
    private val activity: Activity,
    private val nodeOptionsBottomSheetViewModel: NodeOptionsBottomSheetViewModel,
) {

    private val selectMoveNodeActivityLauncher =
        (activity as? AppCompatActivity)?.registerForActivityResult(
            SelectFolderToMoveActivityContract()
        ) { result ->
            result?.let {
                nodeOptionsBottomSheetViewModel.checkNodesNameCollision(
                    it.first.toList(),
                    it.second,
                    NodeNameCollisionType.MOVE
                )
            }
        }

    private val selectCopyNodeActivityLauncher =
        (activity as? AppCompatActivity)?.registerForActivityResult(
            SelectFolderToMoveActivityContract()
        ) { result ->
            result?.let {
                nodeOptionsBottomSheetViewModel.checkNodesNameCollision(
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
                nodeOptionsBottomSheetViewModel.deleteVersionHistory(it)
            }
        }

    private val shareFolderActivityLauncher =
        (activity as? AppCompatActivity)?.registerForActivityResult(
            ShareFolderActivityContract()
        ) { result ->
            result?.let {
                nodeOptionsBottomSheetViewModel.contactSelectedForShareFolder(it)
            }
        }

    private val restoreFromRubbishLauncher =
        (activity as? AppCompatActivity)?.registerForActivityResult(
            SelectFolderToMoveActivityContract()
        ) { result ->
            result?.let {
                nodeOptionsBottomSheetViewModel.checkNodesNameCollision(
                    it.first.toList(),
                    it.second,
                    NodeNameCollisionType.RESTORE
                )
            }
        }

    /**
     * handles actions
     *
     * @param action
     * @param node
     */
    fun handleAction(action: MenuAction, node: TypedNode) {
        when (action) {
            is VersionsMenuAction -> versionsActivityLauncher?.launch(node.id.longValue)
            is MoveMenuAction -> selectMoveNodeActivityLauncher?.launch(longArrayOf(node.id.longValue))
            is CopyMenuAction -> selectCopyNodeActivityLauncher?.launch(longArrayOf(node.id.longValue))
            is ShareFolderMenuAction -> shareFolderActivityLauncher?.launch(longArrayOf(node.id.longValue))
            is RestoreMenuAction -> restoreFromRubbishLauncher?.launch(longArrayOf(node.id.longValue))
            else -> throw NotImplementedError("Action $action does not have a handler.")
        }
    }
}