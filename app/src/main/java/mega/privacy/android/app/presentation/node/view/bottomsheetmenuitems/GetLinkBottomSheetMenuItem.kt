package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import android.content.Intent
import androidx.navigation.NavHostController
import mega.privacy.android.app.getLink.GetLinkActivity
import mega.privacy.android.app.presentation.node.model.menuaction.GetLinkMenuAction
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Get link bottom sheet menu action
 *
 * @param menuAction [GetLinkMenuAction]
 */
class GetLinkBottomSheetMenuItem @Inject constructor(
    override val menuAction: GetLinkMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && node.exportedData?.publicLink.isNullOrEmpty()
            && isNodeInRubbish.not()
            && accessPermission == AccessPermission.OWNER

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
    ): () -> Unit = {
        onDismiss()
        navController.context.startActivity(
            Intent(navController.context, GetLinkActivity::class.java)
                .putExtra(Constants.HANDLE, node.id.longValue)
        )
    }

    override val groupId: Int
        get() = 7
}