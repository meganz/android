package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.list.view.NodeActionListTile
import mega.privacy.android.core.nodecomponents.mapper.NodeLabelResourceMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.LabelMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuitem.components.LabelAccessoryView
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.node.GetNodeLabelUseCase
import java.io.File
import javax.inject.Inject

internal const val changeLabelBottomSheetRoute =
    "search/node_bottom_sheet/change_label_bottom_sheet"

/**
 * Label bottom sheet menu item
 *
 * @param menuAction [LabelMenuAction]
 */
class LabelBottomSheetMenuItem @Inject constructor(
    override val menuAction: LabelMenuAction,
    private val getNodeLabelUseCase: GetNodeLabelUseCase,
    private val labelResourceMapper: NodeLabelResourceMapper,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {

    override fun buildComposeControl(
        selectedNode: TypedNode,
    ): BottomSheetClickHandler =
        { onDismiss, handler, navController, scope ->
            NodeActionListTile(
                text = menuAction.getDescription(),
                icon = menuAction.getIconPainter(),
                isDestructive = isDestructiveAction,
                onActionClicked = getOnClickFunction(
                    node = selectedNode,
                    onDismiss = onDismiss,
                    actionHandler = handler,
                    navController = navController,
                    parentCoroutineScope = scope
                ),
                trailingItem = {
                    val nodeLabel = getNodeLabelUseCase(selectedNode.label)
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
            && accessPermission in setOf(AccessPermission.FULL, AccessPermission.OWNER)
            && isInBackups.not()

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuActionWithIcon, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        navController.navigate(
            route = changeLabelBottomSheetRoute.plus(File.separator).plus(node.id.longValue)
        )
    }

    override val groupId: Int
        get() = 3
}