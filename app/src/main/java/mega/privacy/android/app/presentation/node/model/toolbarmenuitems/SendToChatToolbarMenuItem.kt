package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.node.model.menuaction.SendToChatMenuAction
import mega.privacy.android.core.ui.model.MenuAction
import mega.privacy.android.core.ui.model.MenuActionWithIcon
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.qualifier.ApplicationScope
import mega.privacy.android.domain.usecase.chat.GetNodeToAttachUseCase
import javax.inject.Inject

/**
 * Send to chat menu item
 */
class SendToChatToolbarMenuItem @Inject constructor(
    override val menuAction: SendToChatMenuAction,
    private val getNodeToAttachUseCase: GetNodeToAttachUseCase,
    @ApplicationScope private val scope: CoroutineScope,
) : NodeToolbarMenuItem<MenuActionWithIcon> {

    override fun shouldDisplay(
        hasNodeAccessPermission: Boolean,
        selectedNodes: List<TypedNode>,
        canBeMovedToTarget: Boolean,
        noNodeInBackups: Boolean,
        noNodeTakenDown: Boolean,
        allFileNodes: Boolean,
        resultCount: Int,
    ) = allFileNodes && noNodeTakenDown

    override fun getOnClick(
        selectedNodes: List<TypedNode>,
        onDismiss: () -> Unit,
        actionHandler: (menuAction: MenuAction, nodes: List<TypedNode>) -> Unit,
        navController: NavHostController,
    ): () -> Unit = {
        onDismiss()
        scope.launch {
            val attachableNodes = selectedNodes.mapNotNull {
                if (it is TypedFileNode) {
                    getNodeToAttachUseCase(it)
                } else null
            }
            actionHandler(menuAction, attachableNodes)
        }
    }

}