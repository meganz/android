package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.compose.runtime.Composable
import mega.android.core.ui.components.toggle.Toggle
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.core.nodecomponents.menu.menuaction.AvailableOfflineMenuAction
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.foldernode.IsFolderEmptyUseCase
import javax.inject.Inject

/**
 * Available offline menu item
 *
 * @param menuAction [AvailableOfflineMenuAction]
 */
class AvailableOfflineBottomSheetMenuItem @Inject constructor(
    override val menuAction: AvailableOfflineMenuAction,
    private val isFolderEmptyUseCase: IsFolderEmptyUseCase,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {

    override fun buildComposeControl(
        selectedNode: TypedNode,
    ): @Composable (BottomSheetClickHandler) -> Unit = { handler ->
        val onClick = getOnClickFunction(
            node = selectedNode,
            handler = handler
        )
        NodeActionListTile(
            text = menuAction.getDescription(),
            icon = menuAction.getIconPainter(),
            isDestructive = isDestructiveAction,
            onActionClicked = onClick,
            trailingItem = {
                Toggle(
                    isChecked = selectedNode.isAvailableOffline,
                    onCheckedChange = { onClick() },
                )
            }
        )
    }

    override fun getOnClickFunction(
        node: TypedNode,
        handler: BottomSheetClickHandler,
    ): () -> Unit = {
        // Don't dismiss the bottom sheet here
        handler.actionHandler(menuAction, node)
    }

    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = isNodeInRubbish.not() && node.isTakenDown.not() && isFolderEmptyUseCase(node).not()

    override val groupId = 6
}