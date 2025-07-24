package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import android.content.Intent
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.getLink.GetLinkActivity
import mega.privacy.android.app.presentation.node.model.menuaction.ManageLinkMenuAction
import mega.privacy.android.app.utils.Constants
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Manage link menu item
 *
 * This is a special case of manage link where we show manage link if
 *      only one item selected and link is created for it
 *      or multiple items selected
 *      its only available from cloud drive screen
 */
class MultiSelectManageLinkToolbarMenuItem @Inject constructor() :
    NodeToolbarMenuItem<MenuActionWithIcon> {

    override val menuAction = ManageLinkMenuAction()
    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = noNodeTakenDown
            && hasNodeAccessPermission
            && ((selectedNodes.size == 1 && selectedNodes.first().exportedData != null) || selectedNodes.size > 1) //if size 1 and exported data null we show GetLink

    override fun getOnClick(
        selectedNodes: List<TypedNode>,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit,
        navController: NavHostController,
        parentScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        val nodeList = selectedNodes.map { it.id.longValue }.toLongArray()
        val manageLinkIntent = Intent(navController.context, GetLinkActivity::class.java)
            .putExtra(Constants.HANDLE_LIST, nodeList)
        navController.context.startActivity(manageLinkIntent)
    }

}
