package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.compose.runtime.Composable
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.dialog.contact.CannotVerifyContactDialogArgs
import mega.privacy.android.core.nodecomponents.extension.isOutShare
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.core.nodecomponents.menu.menuaction.VerifyMenuAction
import mega.privacy.android.core.nodecomponents.model.BottomSheetClickHandler
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.shares.GetUnverifiedIncomingShares
import mega.privacy.android.domain.usecase.shares.GetUnverifiedOutgoingShares
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject

/**
 * Verify bottom sheet menu item
 *
 * @param menuAction [VerifyMenuAction]
 */
class VerifyBottomSheetMenuItem @Inject constructor(
    override val menuAction: VerifyMenuAction,
    private val getUnverifiedIncomingShares: GetUnverifiedIncomingShares,
    private val getUnverifiedOutgoingShares: GetUnverifiedOutgoingShares,
    private val megaNavigator: MegaNavigator,
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
        shareData = when {
            node.isOutShare() -> runCatching {
                getUnverifiedOutgoingShares(SortOrder.ORDER_NONE).firstOrNull {
                    it.nodeHandle == node.id.longValue
                }
            }.getOrNull()

            node.isIncomingShare -> runCatching {
                getUnverifiedIncomingShares(SortOrder.ORDER_NONE).firstOrNull {
                    it.nodeHandle == node.id.longValue
                }
            }.getOrNull()

            else -> null
        }
        return shareData?.user.isNullOrEmpty().not()
    }

    override fun getOnClickFunction(
        node: TypedNode,
        handler: BottomSheetClickHandler,
    ): () -> Unit = {
        handler.onDismiss()
        shareData?.let { data ->
            if (data.isVerified.not() && data.isPending) {
                handler.navigationHandler.navigate(
                    CannotVerifyContactDialogArgs(data.user.orEmpty())
                )
            } else {
                // If the share is not pending, we need to show the credentials activity
                megaNavigator.openAuthenticityCredentialsActivity(
                    context = handler.context,
                    email = data.user.orEmpty(),
                    isIncomingShares = node.isIncomingShare
                )
            }
        }
    }

    override val groupId = 2
}