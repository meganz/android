package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.presentation.node.model.menuaction.DisputeTakeDownMenuAction
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Dispute menu item
 *
 * @property menuAction [DisputeTakeDownMenuAction]
 */
class DisputeTakeDownMenuItem @Inject constructor(
    override val menuAction: DisputeTakeDownMenuAction,
) : NodeToolbarMenuItem<MenuActionWithIcon> {

    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = selectedNodes.size == 1
            && noNodeTakenDown.not()

    override fun getOnClick(
        selectedNodes: List<TypedNode>,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit,
        navController: NavHostController,
        parentScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        val disputeTakeDownIntent = Intent(navController.context, WebViewActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .setData(Uri.parse(Constants.DISPUTE_URL))
        navController.context.startActivity(disputeTakeDownIntent)
    }
}