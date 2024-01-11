package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import mega.privacy.android.app.presentation.node.model.mapper.NodeLabelResourceMapper
import mega.privacy.android.app.presentation.node.model.menuaction.LabelMenuAction
import mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems.components.LabelAccessoryView
import mega.privacy.android.app.presentation.search.navigation.changeLabelBottomSheetRoute
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.data.mapper.node.label.NodeLabelMapper
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionListTile
import javax.inject.Inject

/**
 * Label bottom sheet menu item
 *
 * @param menuAction [LabelMenuAction]
 */
class LabelBottomSheetMenuItem @Inject constructor(
    override val menuAction: LabelMenuAction,
    private val nodeLabelMapper: NodeLabelMapper,
    private val labelResourceMapper: NodeLabelResourceMapper,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {

    override fun buildComposeControl(
        selectedNode: TypedNode,
    ): BottomSheetClickHandler =
        { onDismiss, handler, navController ->
            MenuActionListTile(
                text = menuAction.getDescription(),
                icon = menuAction.getIconPainter(),
                isDestructive = isDestructiveAction,
                onActionClicked = getOnClickFunction(
                    node = selectedNode,
                    onDismiss = onDismiss,
                    actionHandler = handler,
                    navController = navController,
                ),
                addSeparator = false,
                trailingItem = {
                    val nodeLabel = nodeLabelMapper(selectedNode.label)
                    val resource = nodeLabel?.let {
                        labelResourceMapper(
                            nodeLabel = it,
                            selectedLabel = null
                        )
                    }
                    resource?.let {
                        LabelAccessoryView(
                            text = stringResource(id = it.labelName),
                            color = colorResource(it.labelColor)
                        )
                    }
                }
            )
        }

    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && isNodeInRubbish.not()
            && accessPermission in listOf(AccessPermission.FULL, AccessPermission.OWNER)
            && isInBackups.not()

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
    ): () -> Unit = {
        onDismiss()
        navController.navigate(
            route = changeLabelBottomSheetRoute.plus("/${node.id.longValue}")
        )
    }

    override val groupId: Int
        get() = 3
}