package mega.privacy.android.core.nodecomponents.action.clickhandler

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mega.android.core.ui.model.menu.MenuAction
import mega.privacy.android.core.nodecomponents.action.MultipleNodesActionProvider
import mega.privacy.android.core.nodecomponents.action.NodeActionProvider
import mega.privacy.android.core.nodecomponents.action.SingleNodeActionProvider
import mega.privacy.android.core.nodecomponents.mapper.RestoreNodeResultMapper
import mega.privacy.android.core.nodecomponents.menu.menuaction.RestoreMenuAction
import mega.privacy.android.domain.entity.node.NodeNameCollisionType
import mega.privacy.android.domain.entity.node.SingleNodeRestoreResult
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.usecase.node.CheckNodesNameCollisionUseCase
import mega.privacy.android.domain.usecase.node.IsNodeDeletedFromBackupsUseCase
import mega.privacy.android.domain.usecase.node.RestoreNodesUseCase
import timber.log.Timber
import javax.inject.Inject

class RestoreActionClickHandler @Inject constructor(
    private val checkNodesNameCollisionUseCase: CheckNodesNameCollisionUseCase,
    private val restoreNodesUseCase: RestoreNodesUseCase,
    private val restoreNodeResultMapper: RestoreNodeResultMapper,
    private val isNodeDeletedFromBackupsUseCase: IsNodeDeletedFromBackupsUseCase,
) : SingleNodeAction, MultiNodeAction {
    override fun canHandle(action: MenuAction): Boolean = action is RestoreMenuAction

    override fun handle(action: MenuAction, node: TypedNode, provider: SingleNodeActionProvider) {
        handleRestore(listOf(node), provider)
    }

    override fun handle(
        action: MenuAction,
        nodes: List<TypedNode>,
        provider: MultipleNodesActionProvider,
    ) {
        handleRestore(nodes, provider)
    }

    private fun handleRestore(
        nodes: List<TypedNode>,
        provider: NodeActionProvider,
    ) {
        provider.coroutineScope.launch {
            withContext(NonCancellable) {
                // backup nodes under syncDebris folder so user only can select one or all backup nodes
                val isNodeInBackups =
                    nodes.isNotEmpty() && runCatching { isNodeDeletedFromBackupsUseCase(nodes.first().id) }
                        .getOrDefault(false)
                if (isNodeInBackups) {
                    // treat as move
                    val nodeHandleArray = nodes.map { it.id.longValue }.toLongArray()
                    provider.moveLauncher.launch(nodeHandleArray)
                } else {
                    val restoreMap = nodes.associate { node ->
                        node.id.longValue to (node.restoreId?.longValue ?: -1L)
                    }
                    runCatching {
                        checkNodesNameCollisionUseCase(restoreMap, NodeNameCollisionType.RESTORE)
                    }.onSuccess { result ->
                        if (result.conflictNodes.isNotEmpty()) {
                            provider.coroutineScope.ensureActive()
                            provider.restoreLauncher.launch(ArrayList(result.conflictNodes.values))
                            provider.viewModel.dismiss()
                        }
                        if (result.noConflictNodes.isNotEmpty()) {
                            val restoreResult = restoreNodesUseCase(result.noConflictNodes)
                            val message = restoreNodeResultMapper(restoreResult)

                            if (restoreResult is SingleNodeRestoreResult) {
                                restoreResult.destinationHandle?.let { destinationHandle ->
                                    postMessageWithAction(
                                        provider,
                                        result.noConflictNodes.keys.first(),
                                        destinationHandle,
                                        message
                                    )
                                } ?: run {
                                    provider.postMessage(message)
                                    provider.viewModel.dismiss()
                                }
                            } else {
                                provider.postMessage(message)
                                provider.viewModel.dismiss()
                            }
                        }
                    }.onFailure { throwable ->
                        Timber.e(throwable)
                        provider.viewModel.dismiss()
                    }
                }
            }
        }
    }

    private fun postMessageWithAction(
        provider: NodeActionProvider,
        restoredNodeHandle: Long,
        destinationHandle: Long,
        message: String,
    ) {
        provider.viewModel.onRestoreSuccess(
            message = message,
            parentHandle = destinationHandle,
            restoredNodeHandle = restoredNodeHandle
        )
    }
}
