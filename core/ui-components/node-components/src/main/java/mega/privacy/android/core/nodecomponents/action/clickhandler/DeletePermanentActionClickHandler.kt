package mega.privacy.android.core.nodecomponents.action.clickhandler

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.NodeActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.dialog.delete.MoveToRubbishOrDeleteDialogArgs
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.DeletePermanentlyMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import timber.log.Timber
import javax.inject.Inject

class DeletePermanentActionClickHandler @Inject constructor(
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is DeletePermanentlyMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        deleteNodes(listOf(node), provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        deleteNodes(nodes, provider)
    }

    private fun deleteNodes(
        nodes: List<TypedNode>,
        provider: NodeActionProvider,
    ) {
        provider.coroutineScope.launch {
            withContext(NonCancellable) {
                val handles = nodes.map { it.id.longValue }
                runCatching { nodeHandlesToJsonMapper(handles) }
                    .onSuccess {
                        provider.navigationHandler?.navigate(
                            MoveToRubbishOrDeleteDialogArgs(
                                isInRubbish = true,
                                nodeHandles = handles
                            )
                        )
                    }
                    .onFailure { Timber.e(it) }
            }
        }
    }
}
