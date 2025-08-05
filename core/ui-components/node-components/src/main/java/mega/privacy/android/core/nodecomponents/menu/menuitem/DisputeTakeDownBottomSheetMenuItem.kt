package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuaction.DisputeTakeDownMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject

/**
 * Dispute take down menu item
 *
 * @param menuAction [DisputeTakeDownMenuAction]
 */
class DisputeTakeDownBottomSheetMenuItem @Inject constructor(
    override val menuAction: DisputeTakeDownMenuAction,
    private val megaNavigator: MegaNavigator,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown


    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuActionWithIcon, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        megaNavigator.launchUrl(navController.context, DISPUTE_URL)
    }

    override val groupId = 4

    companion object {
        // Todo duplicate to the on in mega.privacy.android.app.utils.Constants.java
        private const val DISPUTE_URL: String = "https://mega.io/dispute"
    }
}