package mega.privacy.android.core.nodecomponents.mapper

import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.menu.menuaction.CopyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DeletePermanentlyMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.DownloadMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.HideMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageLinkMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.MoveMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.RestoreMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Mapper that handles the conversion from [NodeSelectionAction] to [mega.android.core.ui.model.menu.MenuAction]
 * and executes the corresponding action using [NodeActionHandler].
 *
 * This mapper eliminates code duplication in screens where the same
 * mapping logic is repeated in multiple onActionPressed callbacks.
 */
class NodeSelectionActionToMenuActionMapper @Inject constructor(
    private val manageLinkMenuAction: ManageLinkMenuAction,
    private val downloadMenuAction: DownloadMenuAction,
    private val hideMenuAction: HideMenuAction,
    private val moveMenuAction: MoveMenuAction,
    private val copyMenuAction: CopyMenuAction,
    private val trashMenuAction: TrashMenuAction,
    private val restoreMenuAction: RestoreMenuAction,
    private val deletePermanentlyMenuAction: DeletePermanentlyMenuAction,
) {
    /**
     * Handles a NodeSelectionAction by mapping it to the appropriate MenuAction
     * and executing it using the provided NodeActionHandler.
     *
     * @param action The NodeSelectionAction to handle
     * @param selectedNodes The list of selected nodes to act upon
     * @param nodeActionHandler The handler to execute the mapped MenuAction
     * @param onMoreActionPressed Callback for handling the More action (shows bottom sheet)
     */
    operator fun invoke(
        action: NodeSelectionAction,
        selectedNodes: List<TypedNode>,
        nodeActionHandler: NodeActionHandler,
        onMoreActionPressed: () -> Unit,
    ) {
        when (action) {
            is NodeSelectionAction.ShareLink -> {
                nodeActionHandler(manageLinkMenuAction, selectedNodes)
            }

            is NodeSelectionAction.Download -> {
                nodeActionHandler(downloadMenuAction, selectedNodes)
            }

            is NodeSelectionAction.Hide -> {
                nodeActionHandler(hideMenuAction, selectedNodes)
            }

            is NodeSelectionAction.Move -> {
                nodeActionHandler(moveMenuAction, selectedNodes)
            }

            is NodeSelectionAction.Copy -> {
                nodeActionHandler(copyMenuAction, selectedNodes)
            }

            is NodeSelectionAction.RubbishBin -> {
                nodeActionHandler(trashMenuAction, selectedNodes)
            }

            is NodeSelectionAction.More -> {
                onMoreActionPressed()
            }

            is NodeSelectionAction.Restore -> {
                nodeActionHandler(restoreMenuAction, selectedNodes)
            }

            is NodeSelectionAction.DeletePermanently -> {
                nodeActionHandler(deletePermanentlyMenuAction, selectedNodes)
            }

            else -> {
                // Handle other actions that might be added in the future
                // For now, this covers SelectAll, Selecting, and any unknown actions
            }
        }
    }
}
