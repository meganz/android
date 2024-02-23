package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.presentation.node.model.menuaction.LeaveShareMenuAction
import mega.privacy.android.app.presentation.search.navigation.searchLeaveShareFolderDialog
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import timber.log.Timber
import javax.inject.Inject

/**
 * Leave share menu item
 *
 * Handles leave share option from incoming shares. checks for isIncomingShare
 * to identify if the selected link is not some inner folders of shared folder
 *
 * @property menuAction [LeaveShareMenuAction]
 */
class LeaveShareToolBarMenuItem @Inject constructor(
    override val menuAction: LeaveShareMenuAction,
    private val stringWithDelimitersMapper: ListToStringWithDelimitersMapper,
) : NodeToolbarMenuItem<MenuActionWithIcon> {

    override fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = noNodeTakenDown && selectedNodes.isNotEmpty()
            && selectedNodes.all { it.isIncomingShare }

    override fun getOnClick(
        selectedNodes: List<TypedNode>,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit,
        navController: NavHostController,
        parentScope: CoroutineScope,
    ): () -> Unit = {

        val nodeHandleList = selectedNodes.map {
            it.id.longValue
        }
        runCatching {
            stringWithDelimitersMapper(nodeHandleList)
        }.onSuccess {
            navController.navigate(
                searchLeaveShareFolderDialog.plus("/${it}")
            )
        }.onFailure {
            Timber.e(it)
        }
        onDismiss()
    }
}