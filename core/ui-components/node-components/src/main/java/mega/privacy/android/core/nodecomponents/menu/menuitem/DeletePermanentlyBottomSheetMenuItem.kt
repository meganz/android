package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.DeletePermanentlyMenuAction
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import timber.log.Timber
import javax.inject.Inject

/**
 * Delete menu item
 *
 * @param menuAction [DeletePermanentlyMenuAction]
 * @param listToStringWithDelimitersMapper [ListToStringWithDelimitersMapper]
 */
class DeletePermanentlyBottomSheetMenuItem @Inject constructor(
    override val menuAction: DeletePermanentlyMenuAction,
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = isNodeInRubbish && isInBackups.not()

    override fun getOnClickFunction(
        node: TypedNode,
        handler: BottomSheetClickHandler
    ): () -> Unit = {
        handler.onDismiss()
        val handles = listOf(node.id.longValue)
        runCatching { nodeHandlesToJsonMapper(handles) }
            .onSuccess {
                // Todo: navigationHandler
                //navController.navigate(route = "$moveToRubbishOrDelete/${true}/${it}")
            }
            .onFailure { Timber.Forest.e(it) }
    }

    override val isDestructiveAction = true
    override val groupId = 9

    companion object {
        // Todo duplicate routes to the one in mega.privacy.android.app.presentation.search.navigation.MoveToRubbishOrDeleteNavigation
        private const val moveToRubbishOrDelete = "search/moveToRubbishOrDelete/isInRubbish"
    }
}