package mega.privacy.android.core.nodecomponents.action.clickhandler

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.menu.menuaction.RemoveOfflineMenuAction
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.offline.RemoveOfflineNodeUseCase
import timber.log.Timber
import javax.inject.Inject

class RemoveOfflineActionClickHandler @Inject constructor(
    private val removeOfflineNodeUseCase: RemoveOfflineNodeUseCase,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is RemoveOfflineMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        provider.coroutineScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    removeOfflineNodeUseCase(nodeId = node.id)
                }.onFailure { Timber.e(it) }
                    .onSuccess { provider.viewModel.dismiss() }
            }
        }
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        provider.coroutineScope.launch {
            withContext(NonCancellable) {
                runCatching {
                    nodes.forEach { node ->
                        removeOfflineNodeUseCase(nodeId = node.id)
                    }
                }.onFailure { Timber.e(it) }
                    .onSuccess { provider.viewModel.dismiss() }
            }
        }
    }
}