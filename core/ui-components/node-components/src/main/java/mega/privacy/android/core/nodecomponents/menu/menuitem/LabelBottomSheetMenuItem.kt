package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.core.nodecomponents.mapper.NodeLabelResourceMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.LabelMenuAction
import mega.privacy.android.core.nodecomponents.menu.menuitem.components.LabelAccessoryView
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.node.GetNodeLabelUseCase
import javax.inject.Inject

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
    ): @Composable (BottomSheetClickHandler) -> Unit = { handler ->
        NodeActionListTile(
            text = menuAction.getDescription(),
            icon = menuAction.getIconPainter(),
            isDestructive = isDestructiveAction,
            onActionClicked = getOnClickFunction(
                node = selectedNode,
                handler = handler
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
            && isInBackups.not() && node.isNodeKeyDecrypted

    override val groupId: Int
        get() = 3
}