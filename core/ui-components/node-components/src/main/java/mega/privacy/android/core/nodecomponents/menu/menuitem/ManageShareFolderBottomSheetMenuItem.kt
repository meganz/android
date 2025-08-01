package mega.privacy.android.core.nodecomponents.menu.menuitem

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.entity.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageShareFolderMenuAction
import mega.privacy.android.core.nodecomponents.util.isOutShare
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.MegaNavigator
import javax.inject.Inject

/**
 * Manage share folder bottom sheet menu item
 *
 * @param menuAction [ManageShareFolderMenuAction]
 */
class ManageShareFolderBottomSheetMenuItem @Inject constructor(
    override val menuAction: ManageShareFolderMenuAction,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
    private val megaNavigator: MegaNavigator
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = node.isTakenDown.not()
            && accessPermission == AccessPermission.OWNER
            && isNodeInRubbish.not()
            && node.isOutShare()

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuActionWithIcon, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        val context = navController.context
        parentCoroutineScope.launch {
            megaNavigator.openFileContactListActivity(context, node.id.longValue, node.name)
            // Todo feature flag is unavailable, remove when it is available
//            if (getFeatureFlagValueUseCase(AppFeatures.FileContactsComposeUI)) {
//                megaNavigator.openFileContactListActivity(context, node.id.longValue, node.name)
//            } else {
//                megaNavigator.openFileContactListActivity(context, node.id.longValue,)
//            }
        }
    }

    override val groupId = 7

}