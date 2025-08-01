package mega.privacy.android.core.nodecomponents.menu.menuitem

import mega.android.core.ui.components.MegaText
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.nodecomponents.entity.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.entity.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.list.view.NodeActionListTile
import mega.privacy.android.core.nodecomponents.menu.menuaction.VersionsMenuAction
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Versions bottom sheet menu item
 *
 * @param menuAction [VersionsMenuAction]
 */
class VersionsBottomSheetMenuItem @Inject constructor(
    override val menuAction: VersionsMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {

    override fun buildComposeControl(
        selectedNode: TypedNode,
    ): BottomSheetClickHandler =
        { onDismiss, handler, navController, scope ->
            val onClick = getOnClickFunction(
                node = selectedNode,
                onDismiss = onDismiss,
                actionHandler = handler,
                navController = navController,
                parentCoroutineScope = scope
            )
            NodeActionListTile(
                text = menuAction.getDescription(),
                icon = menuAction.getIconPainter(),
                isDestructive = isDestructiveAction,
                onActionClicked = onClick,
                trailingItem = {
                    MegaText(
                        text = selectedNode.versionCount.toString(),
                        textColor = TextColor.Secondary
                    )
                }
            )
        }

    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node is TypedFileNode
            && node.hasVersion
            && node.isTakenDown.not()

    override val groupId = 3
}