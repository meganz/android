package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import android.content.Intent
import android.net.Uri
import androidx.navigation.NavHostController
import mega.privacy.android.app.activities.WebViewActivity
import mega.privacy.android.app.presentation.node.model.menuaction.DisputeTakeDownMenuAction
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Dispute take down menu item
 *
 * @param menuAction [DisputeTakeDownMenuAction]
 */
class DisputeTakeDownBottomSheetMenuItem @Inject constructor(
    override val menuAction: DisputeTakeDownMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown


    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
    ): () -> Unit = {
        onDismiss()
        navController.context.startActivity(
            Intent(navController.context, WebViewActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setData(Uri.parse(Constants.DISPUTE_URL))
        )
    }

    override val groupId = 4
}