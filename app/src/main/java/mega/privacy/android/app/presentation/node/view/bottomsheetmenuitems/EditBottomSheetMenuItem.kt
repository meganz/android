package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.app.MimeTypeList
import mega.privacy.android.app.presentation.node.model.menuaction.EditMenuAction
import mega.privacy.android.app.utils.Constants.SEARCH_ADAPTER
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.NodeId
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.AccessPermission.FULL
import mega.privacy.android.domain.entity.shares.AccessPermission.OWNER
import mega.privacy.android.domain.entity.shares.AccessPermission.READWRITE
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.OpenTextEditorParams
import javax.inject.Inject

/**
 * Edit bottom sheet menu action for opening text files in the text editor.
 * Navigation is performed via the injected [MegaNavigator].
 *
 * @param menuAction [EditMenuAction]
 * @param megaNavigator Used to open the text editor activity.
 */
class EditBottomSheetMenuItem @Inject constructor(
    override val menuAction: EditMenuAction,
    private val megaNavigator: MegaNavigator,
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
            && accessPermission in listOf(OWNER, READWRITE, FULL) && node.isNodeKeyDecrypted

    override val groupId = 1

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        megaNavigator.openTextEditor(
            context = navController.context,
            params = OpenTextEditorParams.CloudNode(
                nodeId = NodeId(node.id.longValue),
                nodeSourceType = SEARCH_ADAPTER,
                mode = TextEditorMode.Edit,
                fileName = node.name,
            ),
        )
        onDismiss()
    }
}
