package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveShareMenuAction
import mega.privacy.android.core.nodecomponents.extension.isOutShare
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import timber.log.Timber
import javax.inject.Inject

/**
 * Remove share bottom sheet menu item
 * @property stringWithDelimitersMapper [ListToStringWithDelimitersMapper]
 */
class RemoveShareBottomSheetMenuItem @Inject constructor(
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && node.isOutShare()
            && isNodeInRubbish.not()

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuActionWithIcon, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        val nodeList = listOf(node.id.longValue)
        runCatching { nodeHandlesToJsonMapper(nodeList) }
            .onSuccess {
                navController.navigate(
                    searchRemoveFolderShareDialog.plus("/${it}")
                )
            }.onFailure {
                Timber.e(it)
            }
    }

    override val menuAction = RemoveShareMenuAction(210)
    override val groupId = 7

    companion object {
        // Todo duplicate to the one in mega.privacy.android.app.presentation.search.model.navigation.RemoveShareFolderNavigation.kt
        private const val searchRemoveFolderShareDialog = "search/folder_share_remove"
    }
}