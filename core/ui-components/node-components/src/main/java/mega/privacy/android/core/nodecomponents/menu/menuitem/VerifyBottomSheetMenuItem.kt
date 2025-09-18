package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.compose.runtime.Composable
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.core.nodecomponents.menu.menuaction.VerifyMenuAction
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.shares.GetNodeShareDataUseCase
import javax.inject.Inject

/**
 * Verify bottom sheet menu item
 *
 * @param menuAction [VerifyMenuAction]
 */
class VerifyBottomSheetMenuItem @Inject constructor(
    override val menuAction: VerifyMenuAction,
    private val getNodeShareDataUseCase: GetNodeShareDataUseCase,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {

    private var shareData: ShareData? = null

    override fun buildComposeControl(
        selectedNode: TypedNode,
    ): @Composable (BottomSheetClickHandler) -> Unit = { handler ->
        NodeActionListTile(
            text = menuAction.getDescription().format(shareData?.user.orEmpty()),
            icon = menuAction.getIconPainter(),
            isDestructive = isDestructiveAction,
            onActionClicked = getOnClickFunction(
                node = selectedNode,
                handler = handler
            ),
        )
    }

    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ): Boolean {
        shareData = getNodeShareDataUseCase(node)
        return shareData?.user.isNullOrEmpty().not()
    }

    override val groupId = 2
}