package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.presentation.node.model.menuaction.DeletePermanentlyMenuAction
import mega.privacy.android.app.presentation.search.navigation.moveToRubbishOrDelete
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
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
    private val listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
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
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        val handles = listOf(node.id.longValue)
        runCatching { listToStringWithDelimitersMapper(handles) }
            .onSuccess { navController.navigate(route = "$moveToRubbishOrDelete/${true}/${it}") }
            .onFailure { Timber.e(it) }
    }

    override val isDestructiveAction = true
    override val groupId = 9
}