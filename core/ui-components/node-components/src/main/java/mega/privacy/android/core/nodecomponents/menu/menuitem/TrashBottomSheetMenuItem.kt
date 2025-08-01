package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.entity.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import timber.log.Timber
import javax.inject.Inject

/**
 * Trash bottom sheet menu item
 *
 * @param menuAction [TrashMenuAction]
 * @param listToStringWithDelimitersMapper [ListToStringWithDelimitersMapper]
 */
class TrashBottomSheetMenuItem @Inject constructor(
    override val menuAction: TrashMenuAction,
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
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
        actionHandler: (menuAction: MenuActionWithIcon, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        val handles = listOf(node.id.longValue)
        runCatching { nodeHandlesToJsonMapper(handles) }
            .onSuccess { navController.navigate(route = "$moveToRubbishOrDelete/${false}/${it}") }
            .onFailure { Timber.e(it) }
    }

    override val isDestructiveAction: Boolean
        get() = true

    override val groupId = 9

    companion object {
        // Todo duplicate to the one in mega.privacy.android.app.presentation.search.model.navigation.SearchMoveToRubbishNavigation.kt
        private const val moveToRubbishOrDelete = "search/moveToRubbishOrDelete/isInRubbish"
    }
}