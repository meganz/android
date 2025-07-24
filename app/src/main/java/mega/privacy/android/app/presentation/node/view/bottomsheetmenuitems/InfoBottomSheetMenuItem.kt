package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import android.content.Intent
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.app.presentation.fileinfo.FileInfoActivity
import mega.privacy.android.app.presentation.node.model.menuaction.InfoMenuAction
import mega.privacy.android.app.utils.Constants
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import javax.inject.Inject

/**
 * Info bottom sheet menu action
 *
 * @param menuAction [InfoMenuAction]
 */
class InfoBottomSheetMenuItem @Inject constructor(
    override val menuAction: InfoMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = true

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        navController.context.apply {
            val fileInfoIntent = Intent(this, FileInfoActivity::class.java).apply {
                putExtra(Constants.HANDLE, node.id.longValue)
            }
            this.startActivity(fileInfoIntent)
        }
    }

    override val groupId: Int
        get() = 3
}