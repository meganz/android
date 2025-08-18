package mega.privacy.android.core.nodecomponents.menu.menuitem

import android.content.Context
import androidx.navigation.NavHostController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuaction.GetLinkMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.navigation.contract.NavigationHandler

/**
 * Get link bottom sheet menu action
 *
 * @param menuAction [GetLinkMenuAction]
 */
class GetLinkBottomSheetMenuItem @Inject constructor(
    @ApplicationContext private val context: Context,
    override val menuAction: GetLinkMenuAction,
    private val megaNavigator: MegaNavigator
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && node.exportedData?.publicLink.isNullOrEmpty()
            && isNodeInRubbish.not()
            && accessPermission == AccessPermission.OWNER

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: NodeActionHandler,
        navigationHandler: NavigationHandler,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        megaNavigator.openGetLinkActivity(
            context = context,
            handle = node.id.longValue
        )
    }

    override val groupId: Int
        get() = 7
}