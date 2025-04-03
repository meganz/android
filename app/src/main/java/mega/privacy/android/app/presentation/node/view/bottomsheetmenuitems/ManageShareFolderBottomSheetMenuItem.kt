package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import android.content.Intent
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.featuretoggle.AppFeatures
import mega.privacy.android.app.presentation.extensions.isOutShare
import mega.privacy.android.app.presentation.filecontact.FileContactListActivity
import mega.privacy.android.app.presentation.filecontact.FileContactListComposeActivity
import mega.privacy.android.app.presentation.node.model.menuaction.ManageShareFolderMenuAction
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.shared.original.core.ui.model.MenuAction
import mega.privacy.android.shared.original.core.ui.model.MenuActionWithIcon
import javax.inject.Inject

/**
 * Manage share folder bottom sheet menu item
 *
 * @param menuAction [ManageShareFolderMenuAction]
 */
class ManageShareFolderBottomSheetMenuItem @Inject constructor(
    override val menuAction: ManageShareFolderMenuAction,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
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
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        val context = navController.context
        parentCoroutineScope.launch {
            val intent = if (getFeatureFlagValueUseCase(AppFeatures.FileContactsComposeUI)) {
                Intent(context, FileContactListComposeActivity::class.java).apply {
                    putExtra(Constants.NAME, node.id.longValue)
                }
            } else {
                FileContactListActivity.launchIntent(
                    context,
                    node.id.longValue
                )
            }
            context.startActivity(intent)
        }
    }

    override val groupId = 7

}


