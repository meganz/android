package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.entity.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.LeaveShareMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import timber.log.Timber
import javax.inject.Inject

/**
 * Leave share bottom sheet menu item
 *
 * @param menuAction [LeaveShareMenuAction]
 */
class LeaveShareBottomSheetMenuItem @Inject constructor(
    override val menuAction: LeaveShareMenuAction,
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && node.isIncomingShare
            && isInBackups.not()

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuActionWithIcon, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        val nodeHandleList = listOf(node.id.longValue)
        runCatching {
            nodeHandlesToJsonMapper(nodeHandleList)
        }.onSuccess {
            navController.navigate(
                searchLeaveShareFolderDialog.plus("/${it}")
            )
        }
    }

    override val isDestructiveAction: Boolean
        get() = true
    override val groupId: Int
        get() = 9

    companion object {
        // Todo duplicate routes to the one in mega.privacy.android.app.presentation.search.navigation.LeaveShareFolderNavigation
        private const val searchLeaveShareFolderDialog = "search/leave_share_folder"
    }
}