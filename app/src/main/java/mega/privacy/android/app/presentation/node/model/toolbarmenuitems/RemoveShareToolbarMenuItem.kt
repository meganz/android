package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.presentation.extensions.isOutShare
import mega.privacy.android.app.presentation.node.model.menuaction.RemoveShareMenuAction
import mega.privacy.android.app.presentation.search.navigation.searchRemoveFolderShareDialog
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import timber.log.Timber
import javax.inject.Inject

/**
 * Remove share menu item
 * @property stringWithDelimitersMapper [ListToStringWithDelimitersMapper]
 */
class RemoveShareToolbarMenuItem @Inject constructor(
    private val stringWithDelimitersMapper: ListToStringWithDelimitersMapper,
) : NodeToolbarMenuItem<MenuActionWithIcon> {

    override suspend fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ): Boolean = selectedNodes.run {
        isNotEmpty() && all { it.isOutShare() }
    }


    override val menuAction = RemoveShareMenuAction(150)

    override fun getOnClick(
        selectedNodes: List<TypedNode>,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit,
        navController: NavHostController,
        parentScope: CoroutineScope,
    ): () -> Unit = {
        val nodeList = selectedNodes.map { it.id }
        runCatching { stringWithDelimitersMapper(nodeList) }
            .onSuccess {
                navController.navigate(
                    searchRemoveFolderShareDialog.plus("/${it}")
                )
            }.onFailure {
                Timber.e(it)
            }
        onDismiss()
    }
}