package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.presentation.node.model.menuaction.RemoveMenuAction
import mega.privacy.android.app.presentation.search.navigation.moveToRubbishOrDelete
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import timber.log.Timber
import javax.inject.Inject

/**
 * Remove menu item
 *
 * @property menuAction [RemoveMenuAction]
 * @property listToStringWithDelimitersMapper [ListToStringWithDelimitersMapper]
 */
class RemoveToolbarMenuItem @Inject constructor(
    override val menuAction: RemoveMenuAction,
    private val listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
) : NodeToolbarMenuItem<MenuActionWithIcon> {

    override fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = selectedNodes.isNotEmpty()

    override fun getOnClick(
        selectedNodes: List<TypedNode>,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit,
        navController: NavHostController,
        parentScope: CoroutineScope,
    ): () -> Unit = {
        val nodeHandles = selectedNodes.map { it.id.longValue }
        runCatching {
            listToStringWithDelimitersMapper(nodeHandles)
        }.onSuccess {
            navController.navigate(route = "$moveToRubbishOrDelete/${true}/${it}")
        }.onFailure {
            Timber.e(it)
        }
        onDismiss()
    }
}