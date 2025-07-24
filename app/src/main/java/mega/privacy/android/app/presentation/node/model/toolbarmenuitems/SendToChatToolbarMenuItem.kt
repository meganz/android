package mega.privacy.android.app.presentation.node.model.toolbarmenuitems

import androidx.navigation.NavHostController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.privacy.android.app.presentation.node.model.menuaction.SendToChatMenuAction
import mega.privacy.android.domain.entity.node.TypedFileNode
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.chat.GetNodeToAttachUseCase
import mega.android.core.ui.model.menu.MenuAction
import mega.android.core.ui.model.menu.MenuActionWithIcon
import timber.log.Timber
import javax.inject.Inject

/**
 * Send to chat menu item
 */
class SendToChatToolbarMenuItem @Inject constructor(
    override val menuAction: SendToChatMenuAction,
    private val getNodeToAttachUseCase: GetNodeToAttachUseCase,
) : NodeToolbarMenuItem<MenuActionWithIcon> {

    override suspend fun shouldDisplay(
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
        parentScope: CoroutineScope,
    ): () -> Unit = {
        onDismiss()
        parentScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    val attachableNodes = selectedNodes.mapNotNull {
                        if (it is TypedFileNode) {
                            getNodeToAttachUseCase(it)
                        } else null
                    }
                    parentScope.ensureActive()
                    actionHandler(menuAction, attachableNodes)
                }.onFailure {
                    Timber.e(it, "Error getting node to attach")
                }
            }
        }
    }

}