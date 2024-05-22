package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.presentation.node.model.menuaction.RenameMenuAction
import mega.privacy.android.app.presentation.search.navigation.searchRenameDialog
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import javax.inject.Inject

/**
 * Rename toolbar option
 */
class RenameToolbarMenuItem @Inject constructor() : NodeToolbarMenuItem<MenuActionWithIcon> {

    override val menuAction = RenameMenuAction()
    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = hasNodeAccessPermission && selectedNodes.size == 1 && noNodeInBackups

    override fun getOnClick(
        selectedNodes: List<TypedNode>,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit,
        navController: NavHostController,
        parentScope: CoroutineScope,
    ): () -> Unit = {
        val typedNode = selectedNodes.firstOrNull()
        typedNode?.let {
            navController.navigate(searchRenameDialog.plus("/${it.id.longValue}"))
        }
        onDismiss()
    }
}