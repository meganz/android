package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import mega.privacy.android.app.presentation.node.model.menuaction.TrashMenuAction
import mega.privacy.android.app.presentation.search.moveToRubbishOrDelete
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Trash bottom sheet menu item
 *
 * @param menuAction [TrashMenuAction]
 */
class TrashBottomSheetMenuItem @Inject constructor(
    override val menuAction: TrashMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = isNodeInRubbish.not()
            && node.isIncomingShare.not()
            && accessPermission in listOf(
        AccessPermission.OWNER,
        AccessPermission.FULL,
    ) && isInBackups.not()

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
    ): () -> Unit = {
        onDismiss()
        navController.navigate(route = "$moveToRubbishOrDelete/${node.id.longValue}/${false}/${false}")
    }

    override val isDestructiveAction: Boolean
        get() = true

    override val groupId = 9
}