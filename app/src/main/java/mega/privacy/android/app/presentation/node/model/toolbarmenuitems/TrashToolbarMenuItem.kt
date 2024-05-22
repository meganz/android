package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.presentation.node.model.menuaction.TrashMenuAction
import mega.privacy.android.app.presentation.search.navigation.moveToRubbishOrDelete
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import timber.log.Timber
import javax.inject.Inject

/**
 * Rubbish bin menu item
 *
 * @property menuAction [TrashMenuAction]
 * @property listToStringWithDelimitersMapper [ListToStringWithDelimitersMapper]
 */
class TrashToolbarMenuItem @Inject constructor(
    override val menuAction: TrashMenuAction,
    private val listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
) : NodeToolbarMenuItem<MenuAction> {


    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = noNodeInBackups && canBeMovedToTarget &&
            hasNodeAccessPermission && !selectedNodes.any { it.isIncomingShare }

    override fun getOnClick(
        selectedNodes: List<TypedNode>,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit,
        navController: NavHostController,
        parentScope: CoroutineScope,
    ): () -> Unit = {
        val nodeHandles = selectedNodes.map { it.id.longValue }
        runCatching { listToStringWithDelimitersMapper(nodeHandles) }
            .onSuccess {
                navController.navigate(route = "$moveToRubbishOrDelete/${false}/${it}")
            }.onFailure {
                Timber.e(it)
            }
        onDismiss()
    }
}