package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.dialog.removeshare.RemoveShareFolderDialogArgs
import mega.privacy.android.core.nodecomponents.extension.isOutShare
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveShareMenuAction
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
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
        handler: BottomSheetClickHandler
    ): () -> Unit = {
        handler.onDismiss()
        val nodeList = listOf(node.id.longValue)
        runCatching { nodeHandlesToJsonMapper(nodeList) }
            .onSuccess { handles ->
                handler.navigationHandler.navigate(
                    RemoveShareFolderDialogArgs(nodes = handles)
                )
            }.onFailure {
                Timber.e(it)
            }
    }

    override val menuAction = RemoveShareMenuAction(210)
    override val groupId = 7
}