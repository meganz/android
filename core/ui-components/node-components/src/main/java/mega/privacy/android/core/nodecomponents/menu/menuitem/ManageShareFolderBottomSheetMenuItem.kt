package mega.privacy.android.core.nodecomponents.menu.menuitem

import android.content.Context
import androidx.navigation.NavHostController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.extension.isOutShare
import mega.privacy.android.core.nodecomponents.menu.menuaction.ManageShareFolderMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.feature_flags.AppFeatures
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.contract.NavigationHandler
import javax.inject.Inject

/**
 * Manage share folder bottom sheet menu item
 *
 * @param menuAction [ManageShareFolderMenuAction]
 */
class ManageShareFolderBottomSheetMenuItem @Inject constructor(
    @ApplicationContext private val context: Context,
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
        actionHandler: NodeActionHandler,
        navigationHandler: NavigationHandler,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        parentCoroutineScope.launch {
            if (getFeatureFlagValueUseCase(AppFeatures.SingleActivity)) {
                megaNavigator.openFileContactListActivity(context, node.id.longValue, node.name)
            } else {
                megaNavigator.openFileContactListActivity(context, node.id.longValue,)
            }
        }
    }

    override val groupId = 7

}