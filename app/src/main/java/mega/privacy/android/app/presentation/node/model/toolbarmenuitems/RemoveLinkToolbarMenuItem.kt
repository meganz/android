package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.presentation.node.model.menuaction.RemoveLinkMenuAction
import mega.privacy.android.app.presentation.search.model.navigation.removeNodeLinkRoute
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import java.io.File
import javax.inject.Inject

/**
 * Remove link menu item
 *
 * This item will always be placed on the extras/more option
 */
class RemoveLinkToolbarMenuItem @Inject constructor(
    private val listToStringWithDelimitersMapper: ListToStringWithDelimitersMapper,
) : NodeToolbarMenuItem<MenuActionWithIcon> {

    override val menuAction = RemoveLinkMenuAction(170)

    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = noNodeTakenDown
            && selectedNodes.size > 1
            && selectedNodes.first().exportedData != null

    override fun getOnClick(
        selectedNodes: List<TypedNode>,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit,
        navController: NavHostController,
        parentScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        navController.navigate(
            removeNodeLinkRoute.plus(File.separator)
                .plus(listToStringWithDelimitersMapper(selectedNodes.map { it.id.longValue }))
        )
    }
}