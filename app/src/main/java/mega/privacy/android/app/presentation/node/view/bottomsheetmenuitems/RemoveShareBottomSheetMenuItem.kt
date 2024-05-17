package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.presentation.extensions.isOutShare
import mega.privacy.android.app.presentation.node.model.menuaction.RemoveShareMenuAction
import mega.privacy.android.app.presentation.search.navigation.searchRemoveFolderShareDialog
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.feature.sync.data.mapper.ListToStringWithDelimitersMapper
import timber.log.Timber
import javax.inject.Inject

/**
 * Remove share bottom sheet menu item
 * @property stringWithDelimitersMapper [ListToStringWithDelimitersMapper]
 */
class RemoveShareBottomSheetMenuItem @Inject constructor(
    private val stringWithDelimitersMapper: ListToStringWithDelimitersMapper,
) :
    NodeBottomSheetMenuItem<MenuActionWithIcon> {
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
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        val nodeList = listOf(node.id)
        runCatching { stringWithDelimitersMapper(nodeList) }
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
}