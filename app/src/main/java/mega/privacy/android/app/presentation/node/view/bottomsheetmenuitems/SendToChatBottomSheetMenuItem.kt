package mega.privacy.android.app.presentation.node.view.bottomsheetmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.node.model.menuaction.SendToChatMenuAction
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.chat.GetNodeToAttachUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Send to chat bottom sheet menu item
 *
 * @property menuAction [SendToChatMenuAction]
 * @property getNodeToAttachUseCase [GetNodeToAttachUseCase]
 * @property scope [CoroutineScope]
 */
class SendToChatBottomSheetMenuItem @Inject constructor(
    override val menuAction: SendToChatMenuAction,
    private val getNodeToAttachUseCase: GetNodeToAttachUseCase,
    @ApplicationScope private val scope: CoroutineScope,
) : NodeBottomSheetMenuItem<MenuActionWithIcon> {
    override suspend fun shouldDisplay(
        isNodeInRubbish: Boolean,
        accessPermission: AccessPermission?,
        isInBackups: Boolean,
        node: TypedNode,
        isConnected: Boolean,
    ) = isConnected
            && node is TypedFileNode
            && node.isTakenDown.not()
            && isNodeInRubbish.not()

    override val groupId = 7

    override fun getOnClickFunction(
        node: TypedNode,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, node: TypedNode) -> Unit,
        navController: NavHostController,
    ): () -> Unit = {
        if (node is TypedFileNode) {
            scope.launch {
                runCatching {
                    getNodeToAttachUseCase(node)
                }.onSuccess { typedNode ->
                    typedNode?.let { actionHandler(menuAction, it) }
                }.onFailure { Timber.e(it) }
            }
        }
        onDismiss()
    }
}