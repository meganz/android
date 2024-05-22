package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import android.content.Intent
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.getLink.GetLinkActivity
import mega.privacy.android.app.presentation.node.model.menuaction.ManageLinkMenuAction
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Manage link menu item
 */
class ManageLinkToolbarMenuItem @Inject constructor() : NodeToolbarMenuItem<MenuActionWithIcon> {

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
            && selectedNodes.size == 1
            && selectedNodes.first().exportedData != null //if size 1 and exported data null we show GetLink

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
