package mega.privacy.android.app.presentation.node

import android.app.Activity
import android.content.Intent
import mega.privacy.android.app.getLink.GetLinkActivity
import mega.privacy.android.app.main.VersionsFileActivity
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.presentation.node.model.menuaction.GetLinkMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.InfoMenuAction
import mega.privacy.android.app.presentation.node.model.menuaction.VersionsMenuAction
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode

/**
 * Node bottom sheet action handler
 *
 * @property activity
 */
@Deprecated(
    """
    This class is a temporary solution to the issue that the screens called by the node bottom sheet 
    items have not yet been refactored. As screens are refactored, the code here needs to be 
    replaced by the individual actions defined in the NodeBottomSheetMenuItem implementations
    """
)
class NodeBottomSheetActionHandler(private val activity: Activity) {

    /**
     * handles actions
     *
     * @param action
     * @param node
     */
    fun handleAction(action: MenuAction, node: TypedNode) {
        when (action) {
            is VersionsMenuAction -> {
                val version = Intent(activity, VersionsFileActivity::class.java)
                version.putExtra(Constants.HANDLE, node.id.longValue)
                activity.startActivityForResult(
                    version,
                    Constants.REQUEST_CODE_DELETE_VERSIONS_HISTORY
                )
            }

            is InfoMenuAction -> {
                val fileInfoIntent = Intent(activity, FileInfoActivity::class.java)
                fileInfoIntent.putExtra(Constants.HANDLE, node.id.longValue)
                activity.startActivity(fileInfoIntent)
            }

            is GetLinkMenuAction -> {
                activity.startActivity(
                    Intent(activity, GetLinkActivity::class.java)
                        .putExtra(Constants.HANDLE, node.id.longValue)
                )
            }

            else -> throw NotImplementedError("Action $action does not have a handler.")
        }
    }
}