package mega.privacy.android.core.nodecomponents.menu.menuitem

import android.content.Context
import androidx.navigation.NavHostController
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import mega.android.core.ui.model.menu.MenuActionWithIcon
import mega.privacy.android.core.nodecomponents.action.NodeActionHandler
import mega.privacy.android.core.nodecomponents.menu.menuaction.EditMenuAction
import mega.privacy.android.core.nodecomponents.model.NodeBottomSheetMenuItem
import mega.privacy.android.core.nodecomponents.model.NodeSourceTypeInt
import mega.privacy.android.domain.entity.TextFileTypeInfo
import mega.privacy.android.domain.entity.node.FileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.entity.shares.AccessPermission.FULL
import mega.privacy.android.domain.entity.shares.AccessPermission.OWNER
import mega.privacy.android.domain.entity.shares.AccessPermission.READWRITE
import mega.privacy.android.domain.entity.texteditor.TextEditorMode
import mega.privacy.android.domain.usecase.file.GetFileTypeInfoUseCase
import mega.privacy.android.navigation.MegaNavigator
import mega.privacy.android.navigation.contract.NavigationHandler
import java.io.File
import javax.inject.Inject

/**
 * Edit bottom sheet menu action
 *
 * @param menuAction [EditMenuAction]
 */
class EditBottomSheetMenuItem @Inject constructor(
    @ApplicationContext private val context: Context,
    override val menuAction: EditMenuAction,
    private val megaNavigator: MegaNavigator,
    private val getFileTypeInfoUseCase: GetFileTypeInfoUseCase,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {

    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ): Boolean {
        val filePath = (node as? FileNode)?.fullSizePath ?: return false
        val file = File(filePath)

        return !isNodeInRubbish
                && isInBackups.not()
                && node.isTakenDown.not()
                && getFileTypeInfoUseCase(file) is TextFileTypeInfo
                && accessPermission in listOf(OWNER, READWRITE, FULL)
    }

    override val groupId = 1

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: NodeActionHandler,
        navigationHandler: NavigationHandler,
        parentCoroutineScope: CoroutineScope,
    ): () -> Unit = {
        megaNavigator.openTextEditorActivity(
            context = context,
            currentNodeId = node.id,
            mode = TextEditorMode.Edit,
            nodeSourceType = NodeSourceTypeInt.FILE_BROWSER_ADAPTER,
        )
        onDismiss()
    }
}