package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import android.content.Intent
import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.presentation.node.model.menuaction.EditMenuAction
import mega.privacy.android.app.textEditor.TextEditorActivity
import mega.privacy.android.app.textEditor.TextEditorViewModel
import mega.privacy.android.app.utils.Constants
import mega.privacy.android.app.utils.Constants.SEARCH_ADAPTER
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.AccessPermission.FULL
import mega.privacy.android.domain.entity.shares.AccessPermission.OWNER
import mega.privacy.android.domain.entity.shares.AccessPermission.READWRITE
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import javax.inject.Inject

/**
 * Edit bottom sheet menu action
 *
 * @param menuAction [EditMenuAction]
 */
class EditBottomSheetMenuItem @Inject constructor(
    override val menuAction: EditMenuAction,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = !isNodeInRubbish
            && isInBackups.not()
            && node.isTakenDown.not()
            && node is FileNode
            && MimeTypeList.typeForName(node.name).isOpenableTextFile(node.size)
            && accessPermission in listOf(OWNER, READWRITE, FULL)

    override val groupId = 1

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        val textFileIntent = Intent(navController.context, TextEditorActivity::class.java)
            .apply {
                putExtra(Constants.INTENT_EXTRA_KEY_HANDLE, node.id.longValue)
                putExtra(Constants.INTENT_EXTRA_KEY_ADAPTER_TYPE, SEARCH_ADAPTER)
                putExtra(TextEditorViewModel.MODE, TextEditorMode.Edit.value)
            }
        navController.context.startActivity(textFileIntent)
        onDismiss()
    }
}