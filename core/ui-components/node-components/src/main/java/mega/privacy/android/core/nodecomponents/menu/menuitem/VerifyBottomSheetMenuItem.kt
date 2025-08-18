package mega.privacy.android.core.nodecomponents.menu.menuitem

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
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
import mega.privacy.android.navigation.contract.NavigationHandler
import javax.inject.Inject

/**
 * Verify bottom sheet menu item
 *
 * @param menuAction [VerifyMenuAction]
 */
class VerifyBottomSheetMenuItem @Inject constructor(
    @ApplicationContext private val context: Context,
    override val menuAction: VerifyMenuAction,
    private val getUnverifiedIncomingShares: GetUnverifiedIncomingShares,
    private val getUnverifiedOutgoingShares: GetUnverifiedOutgoingShares,
    private val megaNavigator: MegaNavigator,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {

    private var shareData: ShareData? = null
    override fun buildComposeControl(
        selectedNode: TypedNode,
    ): BottomSheetClickHandler =
        { onDismiss, handler, navigationHandler, scope ->
            NodeActionListTile(
                text = menuAction.getDescription().format(shareData?.user.orEmpty()),
                icon = menuAction.getIconPainter(),
                isDestructive = isDestructiveAction,
                onActionClicked = getOnClickFunction(
                    node = selectedNode,
                    onDismiss = onDismiss,
                    actionHandler = handler,
                    navigationHandler = navigationHandler,
                    parentCoroutineScope = scope,
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
        onDismiss: () -> Unit,
        actionHandler: NodeActionHandler,
        navigationHandler: NavigationHandler,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        shareData?.let {
            if (it.isVerified.not() && it.isPending) {
                // If the share is pending, we need to show cannot verify dialog
                // Todo: navigationHandler
//                navController.navigate(cannotVerifyUserRoute.plus("/${it.user}"))
            } else {
                // If the share is not pending, we need to show the credentials activity
                context.apply {
                    megaNavigator.openAuthenticityCredentialsActivity(
                        context = this,
                        email = it.user.orEmpty(),
                        isIncomingShares = node.isIncomingShare
                    )
                }
            }
        }
    }

    override val groupId = 2

    companion object {
        // Todo duplicate to the one in mega.privacy.android.app.presentation.search.navigation.CannotVerifyUserDialogNavigation.kt
        private const val cannotVerifyUserRoute =
            "search/node_bottom_sheet/cannot_verify_user_dialog"
    }
}