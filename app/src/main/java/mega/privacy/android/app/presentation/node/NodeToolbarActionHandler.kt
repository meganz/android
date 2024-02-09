package mega.privacy.android.app.presentation.node

import android.app.Activity
import mega.privacy.android.app.presentation.node.model.menuaction.DownloadMenuAction
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import timber.log.Timber

/**
 * Node toolbar action handler
 *
 * @property activity
 */

@Deprecated(
    """
    This class is a temporary solution to the issue that the screens called by the node toolbar 
    items have not yet been refactored. As screens are refactored, the code here needs to be 
    replaced by the individual actions defined in the NodeToolbarMenuItem implementations
    """
)
class NodeToolbarActionHandler(
    private val activity: Activity,
) {
    /**
     * handles actions
     *
     * @param action
     * @param nodes
     */
    fun handleAction(action: MenuAction, nodes: List<TypedNode>) {
        when (action) {
            is DownloadMenuAction -> {
                Timber.d("Download action $nodes")
            }

            else -> throw NotImplementedError("Action $action does not have a handler.")
        }
    }
}