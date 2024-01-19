package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import mega.privacy.android.app.presentation.node.model.menuaction.VersionsMenuAction
import mega.privacy.android.core.ui.controls.text.MegaText
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionListTile
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
        { onDismiss, handler, navController ->
            val onClick = getOnClickFunction(
                node = selectedNode,
                onDismiss = onDismiss,
                actionHandler = handler,
                navController = navController,
            )
            MenuActionListTile(
                text = menuAction.getDescription(),
                icon = menuAction.getIconPainter(),
                isDestructive = isDestructiveAction,
                onActionClicked = onClick,
                addSeparator = false,
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