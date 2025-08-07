package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuaction.RenameMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler

/**
 * Rename bottom sheet menu item
 */
class RenameBottomSheetMenuItem @Inject constructor(
    override val menuAction: RenameMenuAction,
) :
    NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = isNodeInRubbish.not() && accessPermission in listOf(
        AccessPermission.OWNER,
        AccessPermission.FULL
    ) && isInBackups.not()

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: NodeActionHandler,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        navController.navigate(searchRenameDialog.plus("/${node.id.longValue}"))
    }

    override val groupId = 8

    companion object {
        // Todo duplicate to the one in mega.privacy.android.app.presentation.search.model.navigation.SearchRenameNavigation.kt
        private const val searchRenameDialog = "search/rename_dialog"
    }
}