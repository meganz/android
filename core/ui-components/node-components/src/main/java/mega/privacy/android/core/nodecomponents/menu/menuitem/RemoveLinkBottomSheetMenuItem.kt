package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.entity.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveLinkMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import java.io.File
import javax.inject.Inject

/**
 * Remove link bottom sheet menu item
 */
class RemoveLinkBottomSheetMenuItem @Inject constructor(
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && node.exportedData?.publicLink != null
            && isNodeInRubbish.not()
            && accessPermission == AccessPermission.OWNER

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuActionWithIcon, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        navController.navigate(
            removeNodeLinkRoute.plus(File.separator)
                .plus(nodeHandlesToJsonMapper(listOf(node.id.longValue)))
        )
    }

    override val menuAction = RemoveLinkMenuAction(170)
    override val groupId = 7

    companion object {
        // Todo duplicate to the one in mega.privacy.android.app.presentation.search.model.navigation.RemoveNodeLinkNavigation.kt
        private const val removeNodeLinkRoute = "search/node_bottom_sheet/remove_node_link_dialog"
    }
}
