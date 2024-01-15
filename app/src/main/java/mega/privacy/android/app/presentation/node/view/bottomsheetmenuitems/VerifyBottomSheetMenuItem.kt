package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import mega.privacy.android.app.presentation.contact.authenticitycredendials.AuthenticityCredentialsActivity
import mega.privacy.android.app.presentation.extensions.isOutShare
import mega.privacy.android.app.presentation.node.model.menuaction.VerifyMenuAction
import mega.privacy.android.app.presentation.search.navigation.cannotVerifyUserRoute
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.ShareData
import mega.privacy.android.domain.entity.SortOrder
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.shares.GetUnverifiedIncomingShares
import mega.privacy.android.domain.usecase.shares.GetUnverifiedOutgoingShares
import mega.privacy.android.legacy.core.ui.controls.lists.MenuActionListTile
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
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {

    private var shareData: ShareData? = null
    override fun buildComposeControl(
        selectedNode: TypedNode,
    ): BottomSheetClickHandler =
        { onDismiss, handler, navController ->
            MenuActionListTile(
                text = menuAction.getDescription().format(shareData?.user.orEmpty()),
                icon = menuAction.getIconPainter(),
                isDestructive = isDestructiveAction,
                onActionClicked = getOnClickFunction(
                    node = selectedNode,
                    onDismiss = onDismiss,
                    actionHandler = handler,
                    navController = navController,
                ),
                addSeparator = false,
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
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
    ): () -> Unit = {
        onDismiss()
        shareData?.let {
            if (it.isVerified.not() && it.isPending) {
                // If the share is pending, we need to show cannot verify dialog
                navController.navigate(cannotVerifyUserRoute.plus("/${it.user}"))
            } else {
                // If the share is not pending, we need to show the credentials activity
                navController.context.apply {
                    startActivity(
                        AuthenticityCredentialsActivity.getIntent(
                            context = this,
                            email = it.user.orEmpty(),
                            isIncomingShares = node.isIncomingShare
                        )
                    )
                }
            }
        }
    }

    override val groupId = 2
}