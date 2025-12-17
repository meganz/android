package mega.privacy.android.core.nodecomponents.action.clickhandler

import kotlinx.coroutines.launch
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.NodeActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.dialog.delete.MoveToRubbishOrDeleteDialogArgs
import mega.privacy.android.core.nodecomponents.mapper.NodeHandlesToJsonMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.TrashMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import timber.log.Timber
import javax.inject.Inject

class MoveToRubbishBinActionClickHandler @Inject constructor(
    private val nodeHandlesToJsonMapper: NodeHandlesToJsonMapper,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is TrashMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        handleTrashAction(listOf(node), provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        handleTrashAction(nodes, provider)
    }

    private fun handleTrashAction(
        nodes: List<TypedNode>,
        provider: NodeActionProvider,
    ) {
        provider.coroutineScope.launch {
            val handles = nodes.map { it.id.longValue }
            runCatching { nodeHandlesToJsonMapper(handles) }
                .onSuccess {
                    provider.viewModel.navigateWithNavKey(
                        MoveToRubbishOrDeleteDialogArgs(
                            isInRubbish = false,
                            nodeHandles = handles
                        )
                    )
                }
                .onFailure { Timber.e(it) }
        }
    }
}
